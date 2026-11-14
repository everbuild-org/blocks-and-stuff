package org.everbuild.blocksandstuff.common.blockinventory

import net.kyori.adventure.text.Component
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack

open class BlockInventory(
    type: InventoryType,
    title: Component,
    protected val backend: PhysicalInventory,
) : Inventory(type, title) {
    /** Shulker boxes reject shulker-box items in the GUI (see ShulkerClickGuard). */
    var rejectsShulkerBoxes: Boolean = false
    init {
        eventNode().addListener(InventoryItemChangeEvent::class.java, this::updateItemInBackend)
    }

    private var syncingFromBackend = false

    private fun updateItemInBackend(event: InventoryItemChangeEvent) {
        if (syncingFromBackend) return
        if (!backend.canPlaceItem(event.slot, event.newItem)) {
            syncingFromBackend = true
            try {
                setItemStack(event.slot, event.previousItem)
            } finally {
                syncingFromBackend = false
            }
            backend.onItemRejected(event.newItem)
            return
        }
        syncingFromBackend = true
        try {
            backend.transact {
                it(event.slot, event.newItem)
            }
        } finally {
            syncingFromBackend = false
        }
    }

    fun updateStackRaw(
        slot: Int,
        stack: ItemStack,
    ) {
        this.setItemStack(slot, stack)
    }
}
