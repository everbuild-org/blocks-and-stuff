package org.everbuild.blocksandstuff.recipes.smithing

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryDrops
import org.everbuild.blocksandstuff.recipes.RecipeFactory.stashController
import org.everbuild.blocksandstuff.recipes.util.excludeResultSlotsFromDrag
import org.everbuild.blocksandstuff.recipes.util.trimPatterns
import kotlin.math.min

class SmithingTableInventory : Inventory(InventoryType.SMITHING, Component.translatable("container.upgrade")), BlockInventoryDrops {
    private var currentRecipe: AbstractSmithingRecipe? = null

    override val dropSlots: Collection<Int> = (TEMPLATE_SLOT..ADDITION_SLOT).toList()

    companion object {
        const val TEMPLATE_SLOT = 0
        const val BASE_SLOT = 1
        const val ADDITION_SLOT = 2
        const val RESULT_SLOT = 3
    }

    init {
        eventNode()
            .addListener(InventoryItemChangeEvent::class.java) { onChangeItem(it) }
            .addListener(InventoryPreClickEvent::class.java) { onClickItem(it) }
            .addListener(InventoryCloseEvent::class.java) { onClose(it.player) }

        eventNode().addListener(InventoryPreClickEvent::class.java) {
            if (it.slot == RESULT_SLOT && !it.player.inventory.cursorItem.isAir) {
                it.isCancelled = true
                return@addListener
            }

            if (it.slot == TEMPLATE_SLOT &&
                (
                    !(trimPatterns.keys + Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE).contains(
                        it.player.inventory.cursorItem
                            .material(),
                    ) &&
                        !it.player.inventory.cursorItem.isAir
                )
            ) {
                it.isCancelled = true
                return@addListener
            }
        }
    }

    fun onChangeItem(event: InventoryItemChangeEvent) {
        if (event.slot == RESULT_SLOT) return
        updateCraftingResult()
    }

    fun onClickItem(event: InventoryPreClickEvent) {
        event.excludeResultSlotsFromDrag(RESULT_SLOT)
        if (event.slot != RESULT_SLOT) return
        event.isCancelled = true
        onCraftItem(event.player, event.click is Click.LeftShift || event.click is Click.RightShift)
    }

    fun onClose(player: Player) {
        currentRecipe = null
    }

    override fun shiftClick(player: Player, slot: Int, button: Int): Boolean {
        if (slot < size) {
            return super.shiftClick(player, slot, button)
        }

        val playerInventory = player.inventory
        val clickSlot = slot - size
        val clicked = playerInventory.getItemStack(clickSlot)
        if (clicked.isAir) return false

        val targetSlot = smithingSlotFor(clicked) ?: return false

        val clickResult =
            clickProcessor.shiftClick(
                playerInventory,
                this,
                targetSlot,
                targetSlot + 1,
                1,
                player,
                clickSlot,
                clicked,
                playerInventory.cursorItem,
            )

        if (clickResult.isCancel) {
            playerInventory.update()
            update(player)
            return false
        }

        playerInventory.setItemStack(clickSlot, clickResult.clicked)
        playerInventory.update()
        update(player)
        playerInventory.cursorItem = clickResult.cursor
        return true
    }

    private fun smithingSlotFor(item: ItemStack): Int? {
        val recipes =
            MinecraftServer
                .getRecipeManager()
                .recipes
                .filterIsInstance<AbstractSmithingRecipe>()

        if (recipes.any { it.template?.matches(item) == true }) return TEMPLATE_SLOT
        if (recipes.any { it.base.matches(item) }) return BASE_SLOT
        if (recipes.any { it.addition?.matches(item) == true }) return ADDITION_SLOT
        return null
    }

    private fun onCraftItem(
        player: Player,
        all: Boolean,
    ) {
        val recipe = currentRecipe ?: return
        val template = getItemStack(TEMPLATE_SLOT)
        val base = getItemStack(BASE_SLOT)
        val addition = getItemStack(ADDITION_SLOT)
        val result = recipe.getResult(template, base, addition)
        val cursorItem = player.inventory.cursorItem

        if (!cursorItem.isAir &&
            (!cursorItem.isSimilar(result) || cursorItem.amount() + result.amount() > cursorItem.maxStackSize())
        ) {
            return
        }

        val templateCount = if (template.isAir) Int.MAX_VALUE else template.amount()
        val baseCount = if (base.isAir) Int.MAX_VALUE else base.amount()
        val additionCount = if (addition.isAir) Int.MAX_VALUE else addition.amount()

        val maxRepetitions = if (all) min(min(templateCount, baseCount), additionCount).coerceAtLeast(1) else 1
        val resultingItem = result.withAmount(maxRepetitions * result.amount())

        if (!template.isAir) this.setItemStack(TEMPLATE_SLOT, template.withAmount(template.amount() - maxRepetitions))
        if (!base.isAir) this.setItemStack(BASE_SLOT, base.withAmount(base.amount() - maxRepetitions))
        if (!addition.isAir) this.setItemStack(ADDITION_SLOT, addition.withAmount(addition.amount() - maxRepetitions))
        this.setItemStack(RESULT_SLOT, ItemStack.AIR)

        if (all) {
            stashController.addToInventoryOrStash(player, resultingItem)
        } else {
            val cursorAmount = if (cursorItem.isAir) 0 else cursorItem.amount()
            val resultingAmount = cursorAmount + resultingItem.amount()
            player.inventory.cursorItem = resultingItem.withAmount(resultingAmount)
        }

        updateCraftingResult()
        update()
    }

    private fun updateCraftingResult() {
        val template = getItemStack(TEMPLATE_SLOT)
        val base = getItemStack(BASE_SLOT)
        val addition = getItemStack(ADDITION_SLOT)

        val recipe =
            MinecraftServer
                .getRecipeManager()
                .recipes
                .filterIsInstance<AbstractSmithingRecipe>()
                .firstOrNull { it.matches(template, base, addition) }
                ?: run {
                    this.setItemStack(RESULT_SLOT, ItemStack.AIR)
                    currentRecipe = null
                    return
                }

        this.setItemStack(RESULT_SLOT, recipe.getResult(template, base, addition))
        currentRecipe = recipe
    }
}
