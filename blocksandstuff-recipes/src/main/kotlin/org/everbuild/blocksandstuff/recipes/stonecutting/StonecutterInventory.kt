package org.everbuild.blocksandstuff.recipes.stonecutting

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
import org.everbuild.averium.org.everbuild.blocksandstuff.recipes.api.StashController
import org.everbuild.averium.org.everbuild.blocksandstuff.recipes.impl.StashControllerImpl
import org.everbuild.blocksandstuff.recipes.util.InventoryButtonClickEvent

class StonecutterInventory(private val stashController: StashController = StashControllerImpl) : Inventory(InventoryType.STONE_CUTTER, Component.translatable("container.stonecutter")) {
    val inputSlot: Int = 0
    val outputSlot: Int = 1
    var recipeItemList: List<ItemStack>? = null
    var lastClickedButton: Int? = null

    init {
        eventNode()
            .addListener(InventoryItemChangeEvent::class.java) { onChangeItem() }
            .addListener(InventoryPreClickEvent::class.java) { onClickItem(it) }
            .addListener(InventoryCloseEvent::class.java) { onClose(it.player) }
            .addListener(InventoryButtonClickEvent::class.java) {
                lastClickedButton = it.getButtonID()
                val recipe = getRecipe(it.getButtonID()) ?: return@addListener
                setItemStack(outputSlot, recipe.result, true)
            }
    }

    fun onChangeItem() {
        val buttonID = lastClickedButton ?: return
        if (getRecipe(buttonID) == null)
            lastClickedButton = null
        updateRecipe(buttonID)
    }

    private fun getRecipe(buttonID: Int): StonecuttingRecipe? {
        val inputItem = getItemStack(inputSlot)

        val recipes = MinecraftServer.getRecipeManager()
            .recipes
            .filterIsInstance<StonecuttingRecipe>()
            .filter { it.matches(inputItem) }
            .sortedByDescending { it.result.material().name() }

        return recipes.getOrNull(buttonID)
    }

    fun updateRecipe(buttonID: Int) {
        val recipe = getRecipe(buttonID)
        if (recipe == null || lastClickedButton == null) {
            lastClickedButton = null
            if (!(getItemStack(outputSlot).isAir))
                setItemStack(outputSlot, ItemStack.AIR.withAmount(2), true)
            return
        }
        setItemStack(outputSlot, recipe.result, true)
        if (getRecipe(buttonID) == null) {
            lastClickedButton = null
            setItemStack(outputSlot, ItemStack.AIR.withAmount(2), true)
        }
    }

    fun onClickItem(event: InventoryPreClickEvent) {
        if (event.slot != outputSlot) return
        event.isCancelled = true
        if (event.clickedItem.isAir) return
        val itemAmount = getItemStack(inputSlot).amount()
        val resultItem = event.clickedItem
        event.inventory.setItemStack(outputSlot, ItemStack.AIR.withAmount(2), true)

        if (event.click == Click.Right(event.slot) || (event.click == Click.Left(event.slot)
                    && (!event.player.inventory.cursorItem.isSimilar(getItemStack(outputSlot))
                    && !event.player.inventory.cursorItem.isAir))
        ) {
            event.isCancelled = true
            return
        } else
            if (event.click == Click.LeftShift(event.slot) || event.click == Click.RightShift(event.slot)) {
                stashController.addToInventoryOrStash(event.entity, resultItem.withAmount(itemAmount))

                setItemStack(inputSlot, ItemStack.AIR.withAmount(2), true)
            } else {
                val amount =
                    if (event.player.inventory.cursorItem.isAir) 0 else event.player.inventory.cursorItem.amount()
                event.player.inventory.cursorItem = resultItem.withAmount(amount + resultItem.amount())
                setItemStack(inputSlot, getItemStack(inputSlot).withAmount(itemAmount - 1), true)
            }
        updateRecipe(lastClickedButton ?: return)
        update()
    }

    fun onClose(player: Player) {
        stashController.addToInventoryOrStash(player, getItemStack(inputSlot))

        recipeItemList = null
    }
}