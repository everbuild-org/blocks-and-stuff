package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

abstract class BlockInventoryHandler : BlockHandler {
    protected fun trackInventory(
        inventory: Inventory,
        position: Point,
        stash: ItemStash? = PlayerInventoryStash,
    ) {
        BlockInventoryTracker.track(inventory, position, stash)
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        BlockInventoryTracker.onDestroy(
            destroy.instance,
            destroy.blockPosition,
            destroy.block,
            destroy.newBlock,
        )
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
