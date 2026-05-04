package org.everbuild.blocksandstuff.recipes.util

import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryButtonClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.network.packet.client.play.ClientClickWindowButtonPacket

object InventoryButtonClickListener {
    fun inventoryButtonClickListener(
        packet: ClientClickWindowButtonPacket,
        player: Player,
    ) {
        val inventory = player.openInventory ?: return
        if (packet.windowId != inventory.windowId.toInt()) return

        EventDispatcher.call(InventoryButtonClickEvent(player, inventory as? Inventory ?: return, packet.buttonId))
    }
}
