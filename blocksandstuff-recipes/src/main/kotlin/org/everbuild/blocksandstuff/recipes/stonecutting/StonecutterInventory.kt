package org.everbuild.blocksandstuff.recipes.stonecutting

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryButtonClickEvent
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.recipe.display.SlotDisplay
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryDrops
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.recipes.api.StashController
import org.everbuild.blocksandstuff.recipes.impl.StashControllerImpl
import org.everbuild.blocksandstuff.recipes.util.excludeResultSlotsFromDrag
import org.everbuild.blocksandstuff.recipes.util.transferInto

class StonecutterInventory(
    private val stashController: StashController = StashControllerImpl,
) : Inventory(InventoryType.STONE_CUTTER, Component.translatable("container.stonecutter")), BlockInventoryDrops {
    val inputSlot: Int = 0
    val outputSlot: Int = 1
    var recipeItemList: List<ItemStack>? = null
    var lastClickedButton: Int? = null

    override val dropSlots: Collection<Int> get() = listOf(inputSlot)

    init {
        eventNode()
            .addListener(InventoryItemChangeEvent::class.java) { onChangeItem() }
            .addListener(InventoryPreClickEvent::class.java) { onClickItem(it) }
            .addListener(InventoryCloseEvent::class.java) { onClose(it.player) }
            .addListener(InventoryButtonClickEvent::class.java) {
                lastClickedButton = it.buttonId
                val recipe = getRecipe(it.buttonId) ?: return@addListener
                setItemStack(outputSlot, recipe.result, true)
            }
    }

    override fun shiftClick(player: Player, slot: Int, button: Int): Boolean {
        val playerInventory = player.inventory

        if (slot == outputSlot) return false

        if (slot < size) {
            // Shift-click inside the stonecutter -> move the item into the player inventory.
            val clicked = getItemStack(slot)
            if (clicked.isAir) return false

            setItemStack(slot, ItemStack.AIR)
            val leftover = playerInventory.addItemStack(clicked, TransactionOption.ALL)
            if (!leftover.isAir) {
                DroppedItemFactory.maybeDropFromPlayer(player, leftover)
            }

            playerInventory.update()
            update(player)
            return true
        }

        // Shift-click from the player inventory -> route into the input slot only, never the result slot.
        val clickSlot = slot - size
        val clicked = playerInventory.getItemStack(clickSlot)
        if (clicked.isAir) return false

        val leftover = transferInto(clicked, this, listOf(inputSlot))
        playerInventory.setItemStack(clickSlot, leftover)
        playerInventory.update()
        update(player)
        return true
    }

    fun onChangeItem() {
        val buttonID = lastClickedButton ?: return
        if (getRecipe(buttonID) == null) {
            lastClickedButton = null
        }
        updateRecipe(buttonID)
    }

    private fun getRecipe(buttonID: Int): StonecuttingRecipe? {
        val inputItem = getItemStack(inputSlot)

        // The client indexes into the stonecutter list in registration order, so resolve the
        // button against that exact order instead of an arbitrary/sorted one.
        val recipes =
            MinecraftServer
                .getRecipeManager()
                .recipes
                .filterIsInstance<StonecuttingRecipe>()
                .filter { it.matches(inputItem) }
                // Minestom only forwards Item/Tag ingredient displays to the client; dropping the
                // others here keeps our index space identical to the client's recipe list.
                .filter { it.ingredientDisplay().let { d -> d is SlotDisplay.Item || d is SlotDisplay.Tag } }
                .sortedBy { it.order }

        return recipes.getOrNull(buttonID)
    }

    fun updateRecipe(buttonID: Int) {
        val recipe = getRecipe(buttonID)
        if (recipe == null || lastClickedButton == null) {
            lastClickedButton = null
            if (!(getItemStack(outputSlot).isAir)) {
                setItemStack(outputSlot, ItemStack.AIR.withAmount(2), true)
            }
            return
        }
        setItemStack(outputSlot, recipe.result, true)
        if (getRecipe(buttonID) == null) {
            lastClickedButton = null
            setItemStack(outputSlot, ItemStack.AIR.withAmount(2), true)
        }
    }

    fun onClickItem(event: InventoryPreClickEvent) {
        event.excludeResultSlotsFromDrag(outputSlot)
        if (event.slot != outputSlot) return
        event.isCancelled = true
        if (event.clickedItem.isAir) return

        val input = getItemStack(inputSlot)
        val result = event.clickedItem
        val cursor = event.player.inventory.cursorItem
        val shift = event.click == Click.LeftShift(event.slot) || event.click == Click.RightShift(event.slot)

        // Right click never takes a result, left click only with an empty or matching cursor.
        if (!shift && (event.click == Click.Right(event.slot) || (!cursor.isAir && !cursor.isSimilar(result)))) {
            return
        }

        if (shift) {
            // Craft the whole input at once, respecting the per-craft result amount.
            setItemStack(outputSlot, ItemStack.AIR, true)
            stashController.addToInventoryOrStash(event.entity, result.withAmount(input.amount() * result.amount()))
            setItemStack(inputSlot, ItemStack.AIR, true)
        } else {
            if (!cursor.isAir && cursor.amount() + result.amount() > cursor.maxStackSize()) return
            setItemStack(outputSlot, ItemStack.AIR, true)
            val amount = (if (cursor.isAir) 0 else cursor.amount()) + result.amount()
            event.player.inventory.cursorItem = result.withAmount(amount)
            setItemStack(inputSlot, input.withAmount(input.amount() - 1))
        }

        updateRecipe(lastClickedButton ?: return)
        update()
    }

    fun onClose(player: Player) {
        recipeItemList = null
    }
}
