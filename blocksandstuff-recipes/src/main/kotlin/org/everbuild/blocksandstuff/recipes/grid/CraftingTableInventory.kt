package org.everbuild.blocksandstuff.recipes.grid

import net.kyori.adventure.text.Component
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType

class CraftingTableInventory() : Inventory(InventoryType.CRAFTING, Component.translatable("container.crafting")) {
    private val service = CraftingTableGridService(this)

    init {
        eventNode()
            .addListener(InventoryItemChangeEvent::class.java) { service.onChangeItem(it) }
            .addListener(InventoryPreClickEvent::class.java) { service.onClickItem(it) }
            .addListener(InventoryCloseEvent::class.java) { service.onClose(it.player) }
    }
}