package org.everbuild.blocksandstuff.recipes.util

import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.trait.InventoryEvent
import net.minestom.server.event.trait.PlayerEvent
import net.minestom.server.inventory.AbstractInventory
import net.minestom.server.inventory.Inventory

class InventoryButtonClickEvent(private val player: Player, private val inventory: Inventory, private val buttonID: Int): Event, PlayerEvent, InventoryEvent {

    override fun getPlayer(): Player {
        return player
    }

    override fun getInventory(): AbstractInventory {
        return inventory
    }

    fun getButtonID(): Int {
        return buttonID
    }
}