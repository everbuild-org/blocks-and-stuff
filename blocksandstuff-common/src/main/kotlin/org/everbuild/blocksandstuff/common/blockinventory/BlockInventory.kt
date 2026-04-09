package org.everbuild.blocksandstuff.common.blockinventory

import net.kyori.adventure.text.Component
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack

open class BlockInventory(
    type: InventoryType,
    title: Component,
    protected val backend: PhysicalInventory
) : Inventory(type, title) {
    init {
        eventNode().addListener(InventoryItemChangeEvent::class.java, this::updateItemInBackend)
    }

    private fun updateItemInBackend(event: InventoryItemChangeEvent) {
        backend.transact {
            it(event.slot, event.newItem)
        }
    }

    fun updateStackRaw(slot: Int, stack: ItemStack) {
        this.setItemStack(slot, stack)
    }
}
