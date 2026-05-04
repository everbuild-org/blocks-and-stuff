package org.everbuild.blocksandstuff.recipes.grid

import net.minestom.server.MinecraftServer.getRecipeManager
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.recipes.RecipeFactory

abstract class AbstractCraftingGridService {
    private var currentRecipe: CraftingGridRecipe? = null

    protected abstract val outputSlot: Int
    protected abstract val width: Int
    protected abstract val height: Int
    protected abstract fun getPattern(): GridPattern<ItemStack>
    protected abstract fun setRecipeItemStack(index: Int, itemStack: ItemStack, sendUpdate: Boolean = true)
    protected abstract fun getRecipeItemStacks(): List<ItemStack>
    protected abstract fun setOutputItemStack(itemStack: ItemStack, sendUpdate: Boolean = true)
    protected abstract fun update()

    fun onChangeItem(event: InventoryItemChangeEvent) {
        if (event.slot == outputSlot) return
        updateCraftingResult()
    }

    fun onClickItem(event: InventoryPreClickEvent) {
        if (event.slot != outputSlot) return
        event.isCancelled = true
        onCraftItem(event.player, event.click is Click.LeftShift || event.click is Click.RightShift)
    }

    open fun onClose(player: Player) {
        getRecipeItemStacks().forEach {
            RecipeFactory.stashController.addToInventoryOrStash(player, it)
        }
        currentRecipe = null
    }

    private fun onCraftItem(player: Player, all: Boolean) {
        val recipe = currentRecipe ?: return
        var grid: GridPattern<ItemStack>? = getPattern().filterInvalid().minimizePattern()
        var outCount = 0
        val result = recipe.getResult(grid!!)
        val cursorItem = player.inventory.cursorItem

        if (cursorItem.isSimilar(result) && cursorItem.maxStackSize() < result.amount() + cursorItem.amount()) {
            return
        }

        do {
            grid = recipe.takeOne(grid!!)
            if (grid == null) break
            outCount++
        } while (all && (outCount * result.amount()) < result.maxStackSize() - result.amount() && recipe.matches(grid))
        val resultingItem = result.withAmount(outCount * result.amount())

        grid?.extendPattern(width, height)
            ?.grid
            ?.flatten()
            ?.mapNotNull { it ?: ItemStack.AIR }
            ?.forEachIndexed { index, ingredient ->
                setRecipeItemStack(index, ingredient)
            }

        if (grid == null) {
            for (index in 0 until (width * height)) {
                setRecipeItemStack(index, ItemStack.AIR)
            }
        }

        update()
        setOutputItemStack(ItemStack.AIR)
        if (all) {
            RecipeFactory.stashController.addToInventoryOrStash(player, resultingItem)
        } else {
            val resultingAmount =
                (if (player.inventory.cursorItem == ItemStack.AIR) 0 else player.inventory.cursorItem.amount()) + resultingItem.amount()
            player.inventory.cursorItem = resultingItem.withAmount(resultingAmount)
        }

        updateCraftingResult()
        update()
    }

    private fun updateCraftingResult() {
        val grid = getPattern().filterInvalid().minimizePattern()

        val recipe = getRecipeManager()
            .recipes
            .filterIsInstance<CraftingGridRecipe>()
            .firstOrNull { it.matches(grid) }
            ?: run {
                setOutputItemStack(ItemStack.AIR)
                currentRecipe = null
                return
            }

        setOutputItemStack(recipe.getResult(grid))
        currentRecipe = recipe
    }
}