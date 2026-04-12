package org.everbuild.blocksandstuff.recipes.smelting

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHolder
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory

abstract class AbstractSmeltingHandler(
    private val recipeType: Class<out SmeltingRecipe>,
    private val archetype: FurnaceArchetype,
) : BlockHandler,
    BlockInventoryHolder {
    private var backend: PhysicalInventory? = null

    override fun getInventory(
        instance: Instance,
        blockPos: Point,
    ): PhysicalInventory {
        backend?.let { return it }
        return archetype.Backend(blockPos, instance).also { backend = it }
    }

    fun getRecipe(inventory: PhysicalInventory): SmeltingRecipeData? {
        val inputSlot = inventory[FurnaceArchetype.SLOT_INPUT]
        if (inputSlot.isAir) return null
        val currentRecipe =
            MinecraftServer
                .getRecipeManager()
                .recipes
                .filterIsInstance(recipeType)
                .firstOrNull { it.matches(inputSlot) } ?: return null
        if (!inventory[FurnaceArchetype.SLOT_OUTPUT].isAir) {
            if (inventory[FurnaceArchetype.SLOT_OUTPUT].amount() >= currentRecipe.result.maxStackSize()) return null
            if (!inventory[FurnaceArchetype.SLOT_OUTPUT].isSimilar(currentRecipe.result)) return null
        }

        val result = currentRecipe.result
        val burnTime = currentRecipe.burnTime
        val experience = currentRecipe.experience
        return SmeltingRecipeData(result, experience, burnTime)
    }

    fun updateCookingTime(
        block: Block,
        recipeData: SmeltingRecipeData?,
    ): Block {
        if (recipeData == null) {
            return block.withTag(cookingTimeSpent, 0).withTag(cookingTotalTime, 0)
        }
        return block.withTag(cookingTimeSpent, block.getTag(cookingTimeSpent) + 1)
    }

    override fun tick(tick: BlockHandler.Tick) {
        var newBlock = tick.block
        var litTime = tick.block.getTag(litTimeRemaining)!!
        val isLit = newBlock.getProperty("lit") == "true"
        val inventory = getInventory(tick.instance, tick.blockPosition)
        if (litTime > 0) {
            val totalCookingTime = newBlock.getTag(cookingTotalTime)
            val spentCookingTime = newBlock.getTag(cookingTimeSpent)

            if (totalCookingTime == 0L) {
                val recipeData = getRecipe(inventory)
                val totalCookingTime = recipeData?.burnTime?.toLong() ?: 0

                newBlock = newBlock.withTag(cookingTotalTime, totalCookingTime).withTag(cookingTimeSpent, 0)
            } else {
                val recipeData = getRecipe(inventory)

                if (spentCookingTime < totalCookingTime) {
                    newBlock = updateCookingTime(newBlock, recipeData)
                } else if (recipeData != null) {
                    tick.instance.setBlock(tick.blockPosition, newBlock, false)
                    insertResult(getInventory(tick.instance, tick.blockPosition), recipeData)
                    newBlock = tick.instance.getBlock(tick.blockPosition)
                    newBlock = newBlock.withTag(cookingTimeSpent, 0).withTag(cookingTotalTime, 0)
                }
            }
            litTime = (litTime - 1).toShort()
            newBlock = newBlock.withProperty("lit", "true").withTag(litTimeRemaining, litTime)
        } else if (getRecipe(inventory) != null) {
            tick.instance.setBlock(tick.blockPosition, newBlock, false)
            val fuelTime = takeFuel(tick.instance, tick.blockPosition)
            if (fuelTime > 0) {
                newBlock = tick.instance.getBlock(tick.blockPosition)
                newBlock =
                    newBlock
                        .withTag(litTotalTime, fuelTime.toLong())
                        .withTag(litTimeRemaining, fuelTime.toShort())
                        .withProperty("lit", "true")
            } else if (isLit) {
                newBlock =
                    newBlock
                        .withProperty("lit", "false")
                        .withTag(cookingTimeSpent, 0)
                        .withTag(cookingTotalTime, 0)
            }
        } else {
            if (isLit) {
                newBlock = newBlock.withProperty("lit", "false")
            }
        }

        if (newBlock != tick.block) {
            tick.instance.setBlock(tick.blockPosition, newBlock, false)
        }
    }

    override fun isTickable(): Boolean = true

    override fun getBlockEntityTags(): MutableCollection<Tag<*>> =
        mutableListOf(
            litTimeRemaining,
            cookingTimeSpent,
            cookingTotalTime,
            litTotalTime,
            customName,
        )

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) {
            return true
        }
        val inventory =
            getInventory(interaction.instance, interaction.blockPosition)
                .getViewableInventory()

        interaction.player.openInventory(inventory)
        return false
    }

    open fun takeFuel(
        instance: Instance,
        blockPosition: Point,
    ): Int {
        val inventory = getInventory(instance, blockPosition)
        return useFuel(inventory)
    }

    companion object {
        val litTimeRemaining: Tag<Short> = Tag.Short("lit_time_remaining").defaultValue(0)
        val cookingTimeSpent: Tag<Long> = Tag.Long("cooking_time_spent").defaultValue(0L)
        val cookingTotalTime: Tag<Long> = Tag.Long("cooking_total_time").defaultValue(0L)
        val litTotalTime: Tag<Long> = Tag.Long("lit_total_time").defaultValue(0L)
        val customName: Tag<List<String>> = Tag.String("custom_name").list()
    }
}
