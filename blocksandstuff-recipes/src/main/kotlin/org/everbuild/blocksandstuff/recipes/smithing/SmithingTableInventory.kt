package org.everbuild.blocksandstuff.recipes.smithing

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.recipes.RecipeFactory.stashController
import kotlin.math.min

class SmithingTableInventory : Inventory(InventoryType.SMITHING, Component.translatable("container.upgrade")) {
    private var currentRecipe: AbstractSmithingRecipe? = null

    companion object {
        const val TEMPLATE_SLOT = 0
        const val BASE_SLOT = 1
        const val ADDITION_SLOT = 2
        const val RESULT_SLOT = 3
    }

    init {
        var globalEventNode: EventNode<Event>? = null

        eventNode()
            .addListener(InventoryItemChangeEvent::class.java) { onChangeItem(it) }
            .addListener(InventoryPreClickEvent::class.java) { onClickItem(it) }
            .addListener(InventoryCloseEvent::class.java) { onClose(it.player) }

        eventNode().addListener(InventoryPreClickEvent::class.java) {
            if (it.slot == RESULT_SLOT && !it.player.inventory.cursorItem.isAir) {
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
        if (event.slot != RESULT_SLOT) return
        event.isCancelled = true
        onCraftItem(event.player, event.click is Click.LeftShift || event.click is Click.RightShift)
    }

    fun onClose(player: Player) {
        (TEMPLATE_SLOT..ADDITION_SLOT)
            .map { getItemStack(it) }
            .filter { !it.isAir }
            .forEach { stashController.addToInventoryOrStash(player, it) }

        currentRecipe = null
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

        if (cursorItem.isSimilar(result) && cursorItem.maxStackSize() < result.amount() + cursorItem.amount()) {
            return
        }

        val templateCount = if (template.isAir) template.amount() else Int.MAX_VALUE
        val baseCount = if (base.isAir) base.amount() else Int.MAX_VALUE
        val additionCount = if (addition.isAir) addition.amount() else Int.MAX_VALUE

        val maxRepetitionsForInput = if (all) min(min(templateCount, baseCount), additionCount) else 1
        val maxRepetitionsForOutput = (result.maxStackSize() - result.amount()).floorDiv(cursorItem.amount())
        val maxRepetitions = min(maxRepetitionsForInput, maxRepetitionsForOutput).coerceAtLeast(1)
        val resultingItem = result.withAmount(maxRepetitions * result.amount())

        val resultingBase = base.withAmount(base.amount() - maxRepetitions)
        val resultingAddition = addition.withAmount(addition.amount() - maxRepetitions)
        val resultingTemplate = template.withAmount(template.amount() - maxRepetitions)

        this.setItemStack(TEMPLATE_SLOT, resultingTemplate)
        this.setItemStack(BASE_SLOT, resultingBase)
        this.setItemStack(ADDITION_SLOT, resultingAddition)
        this.setItemStack(RESULT_SLOT, ItemStack.AIR)

        if (all) {
            stashController.addToInventoryOrStash(player, resultingItem)
        } else {
            val cursorAmount =
                if (player.inventory.cursorItem == ItemStack.AIR) 0 else player.inventory.cursorItem.amount()
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
