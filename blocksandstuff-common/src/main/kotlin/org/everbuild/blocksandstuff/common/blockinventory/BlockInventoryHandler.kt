package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

abstract class BlockInventoryHandler : BlockHandler {
    private val openInventories = mutableMapOf<Inventory, Point>()

    protected fun trackInventory(
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

    /**
     * Opt-in: only inventories implementing [BlockInventoryDrops] get their
     * items returned on close. The [BlockInventoryDrops.dropSlots] define which
     * slots are affected.
     *
     * Items are handed to [stash] (default: player inventory, overflow dropped
     * in front of the player). If no stash is provided, the items are dropped at
     * the block via [DroppedItemFactory].
     */
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

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        if (destroy.newBlock.key() == destroy.block.key()) return

        val affected =
            openInventories.entries
                .filter { it.value.samePoint(destroy.blockPosition) }
                .map { it.key }

        affected.forEach { openInventories.remove(it) }
        closeAndDrop(affected, destroy.instance, destroy.blockPosition)
    }

    companion object {
        fun closeAndDrop(
            inventories: Iterable<Inventory>,
            instance: Instance,
            position: Point,
        ) {
            for (inventory in inventories) {
                val slots = (inventory as? BlockInventoryDrops)?.dropSlots ?: (0 until inventory.size).toList()
                val drops = slots.mapNotNull { slot -> inventory.getItemStack(slot).takeIf { !it.isAir } }

                if (inventory !is BlockInventory) {
                    slots.forEach { inventory.setItemStack(it, ItemStack.AIR, false) }
                }

                inventory.viewers.toList().forEach { player: Player ->
                    if (player.openInventory === inventory) player.closeInventory()
                }

                drops.forEach { DroppedItemFactory.maybeDrop(instance, position, it) }
            }
        }
    }
}
