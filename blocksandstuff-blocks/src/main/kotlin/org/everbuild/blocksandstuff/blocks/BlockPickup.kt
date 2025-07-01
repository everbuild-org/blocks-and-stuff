package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.player.PlayerPickBlockEvent
import net.minestom.server.item.ItemStack

object BlockPickup {
    fun enable() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPickBlockEvent::class.java) {
            val material = it.block.registry().material() ?: return@addListener
            val player = it.player
            val newItemStack = ItemStack.of(material)
            
            if (player.isSneaking) {
                // TODO: Save block entity data
            }
            
            val inventory = player.getInventory()

            for (slot in 0..8) {
                val item = inventory.getItemStack(slot)
                if (item.material() === material) { // TODO: Compare block entity data
                    player.setHeldItemSlot(slot.toByte())
                    return@addListener
                }
            }

            if (player.itemInMainHand.isAir) {
                player.itemInMainHand = newItemStack
                return@addListener
            }

            for (slot in 0..8) {
                val item = inventory.getItemStack(slot)
                if (item.isAir) {
                    player.setHeldItemSlot(slot.toByte())
                    inventory.setItemStack(slot, newItemStack)
                    return@addListener
                }
            }
            
            var firstAirSlot = -1
            for (slot in 0..35) {
                val item = inventory.getItemStack(slot)

                if (item.isAir && firstAirSlot == -1) {
                    firstAirSlot = slot
                }

                if (item.material() === newItemStack.material()) { // TODO: Compare block entity data
                    inventory.setItemStack(slot, player.itemInMainHand)
                    player.setItemInHand(PlayerHand.MAIN, item)
                    return@addListener
                }
            }

            if (firstAirSlot != -1) {
                inventory.setItemStack(firstAirSlot, player.itemInMainHand)
                player.setItemInHand(PlayerHand.MAIN, newItemStack)
            }
        }
    }
}