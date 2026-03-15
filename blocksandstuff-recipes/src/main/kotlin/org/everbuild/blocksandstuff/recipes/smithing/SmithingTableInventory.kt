package org.everbuild.blocksandstuff.recipes.smithing

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.MinecraftServer
import org.everbuild.blocksandstuff.recipes.RecipeFactory.stashController
import kotlin.math.min

class SmithingTableInventory : Inventory(InventoryType.SMITHING, Component.translatable("container.upgrade")) {
    private var currentRecipe: AbstractSmithingRecipe? = null

    init {
        eventNode()
            .addListener(InventoryItemChangeEvent::class.java) { onChangeItem(it) }
            .addListener(InventoryPreClickEvent::class.java) { onClickItem(it) }
            .addListener(InventoryCloseEvent::class.java) { onClose(it.player) }
    }

    fun onChangeItem(event: InventoryItemChangeEvent) {
        if (event.slot == 3) return
        updateCraftingResult()
    }

    fun onClickItem(event: InventoryPreClickEvent) {
        if (event.slot != 3) return
        event.isCancelled = true
        onCraftItem(event.player, event.click is Click.LeftShift || event.click is Click.RightShift)
    }

    fun onClose(player: Player) {
            (0 until 3)
                .map { getItemStack(it) }
                .forEach { stashController.addToInventoryOrStash(player, it) }

        currentRecipe = null
    }

    private fun onCraftItem(player: Player, all: Boolean) {
        val recipe = currentRecipe ?: return
        val template = getItemStack(0)
        val base = getItemStack(1)
        val addition = getItemStack(2)
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

        this.setItemStack(0, resultingTemplate)
        this.setItemStack(1, resultingBase)
        this.setItemStack(2, resultingAddition)
        this.setItemStack(3, ItemStack.AIR)

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
        val template = getItemStack(0)
        val base = getItemStack(1)
        val addition = getItemStack(2)

        val recipe = MinecraftServer.getRecipeManager()
            .recipes
            .filterIsInstance<AbstractSmithingRecipe>()
            .firstOrNull { it.matches(template, base, addition) }
            ?: run {
                this.setItemStack(3, ItemStack.AIR)
                currentRecipe = null
                return
            }

        this.setItemStack(3, recipe.getResult(template, base, addition))
        currentRecipe = recipe
    }
}