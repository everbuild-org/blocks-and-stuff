package org.everbuild.blocksandstuff.common.blockinventory

import net.kyori.adventure.text.Component
import net.minestom.server.inventory.InventoryType

interface BlockInventoryArchetype {
    val title: Component
    val inventoryType: InventoryType
    val size: Int

    fun createInventory(backend: PhysicalInventory): BlockInventory
}