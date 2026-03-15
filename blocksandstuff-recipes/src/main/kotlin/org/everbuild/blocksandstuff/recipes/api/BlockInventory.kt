package org.everbuild.blocksandstuff.recipes.api

import net.kyori.adventure.text.Component
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack

open class BlockInventory(type: InventoryType, title: Component, protected val backend: BlockInventoryBackend) :
    Inventory(type, title) {
    init {
        backend.attach(this)
        eventNode().addListener(InventoryItemChangeEvent::class.java, this::updateItemInBackend)
    }

    private fun updateItemInBackend(event: InventoryItemChangeEvent) {
        backend.itemStacks[event.slot] = event.newItem
        backend.save()
    }

    fun updateStackRaw(slot: Int, stack: ItemStack) {
        itemStacks[slot] = stack
        update()
    }
}
