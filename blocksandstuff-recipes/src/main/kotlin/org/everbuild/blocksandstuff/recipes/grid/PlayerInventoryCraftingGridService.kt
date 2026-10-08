package org.everbuild.blocksandstuff.recipes.grid

import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.item.ItemStack

class PlayerInventoryCraftingGridService(private val player: Player) : AbstractCraftingGridService() {
    override val outputSlot: Int = 36
    override val width: Int = 2
    override val height: Int = 2

    init {
        player.inventory.eventNode()
            .addListener(InventoryItemChangeEvent::class.java) { onChangeItem(it) }
            .addListener(InventoryPreClickEvent::class.java) { onClickItem(it) }
            .addListener(InventoryCloseEvent::class.java) { if (it.inventory.windowId.toInt() == 0) onClose(it.player) }
    }

    override fun getPattern() = GridPattern.fromPlayerInventoryGrid(player.inventory)

    override fun setRecipeItemStack(
        index: Int,
        itemStack: ItemStack,
        sendUpdate: Boolean
    ) {
        player.inventory.setItemStack(index + 37, itemStack, sendUpdate)
    }

    override fun getRecipeItemStacks(): List<ItemStack> {
        return player.inventory.itemStacks.drop(37).take(4)
    }

    override fun setOutputItemStack(itemStack: ItemStack, sendUpdate: Boolean) {
        player.inventory.setItemStack(outputSlot, itemStack, sendUpdate)
    }

    override fun update() {
        player.inventory.update()
    }

    override fun onClose(player: Player) {
        super.onClose(player)
        for (index in 36..40) {
            player.inventory.setItemStack(index, ItemStack.AIR)
        }
    }
}