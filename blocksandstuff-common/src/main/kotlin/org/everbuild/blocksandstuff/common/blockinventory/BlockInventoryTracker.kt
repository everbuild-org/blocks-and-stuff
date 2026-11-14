package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

/**
 * Tracks block-backed inventories so their contents can be returned to the
 * player on close or dropped when the block is destroyed.
 */
object BlockInventoryTracker {
    private val openInventories = mutableMapOf<Inventory, Point>()

    fun track(
        inventory: Inventory,
        position: Point,
        stash: ItemStash? = PlayerInventoryStash,
    ) {
        openInventories[inventory] = position
        inventory.eventNode().addListener(InventoryCloseEvent::class.java) {
            openInventories.remove(inventory)
            returnItemsOnClose(inventory, it.player, position, stash)
        }
    }

    fun onDestroy(instance: Instance, position: Point, oldBlock: Block, newBlock: Block) {
        if (newBlock.key() == oldBlock.key()) return

        val affected =
            openInventories.entries
                .filter { it.value.samePoint(position) }
                .map { it.key }
        affected.forEach { openInventories.remove(it) }
        BlockInventoryHandler.closeAndDrop(affected, instance, position)
    }

    private fun returnItemsOnClose(
        inventory: Inventory,
        player: Player,
        position: Point,
        stash: ItemStash?,
    ) {
        val returnSlots = (inventory as? BlockInventoryDrops)?.dropSlots ?: return
        val instance = player.instance

        for (slot in returnSlots) {
            val item = inventory.getItemStack(slot)
            if (item.isAir) continue

            if (inventory !is BlockInventory) {
                inventory.setItemStack(slot, ItemStack.AIR, false)
            }

            if (stash != null) {
                stash.add(player, item)
            } else if (instance != null) {
                DroppedItemFactory.maybeDrop(instance, position, item)
            }
        }
    }
}
