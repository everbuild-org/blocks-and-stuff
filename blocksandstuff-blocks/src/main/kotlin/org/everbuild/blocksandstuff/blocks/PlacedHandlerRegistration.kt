package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import net.minestom.server.event.instance.InstanceBlockUpdateEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent

object PlacedHandlerRegistration {
    private var registeredHandler: Any? = null

    @JvmStatic
    fun registerDefault() {
        val eventHandler = MinecraftServer.getGlobalEventHandler()
        if (registeredHandler === eventHandler) return
        registeredHandler = eventHandler

        eventHandler.addListener(PlayerBlockPlaceEvent::class.java) {
            val handler = MinecraftServer.getBlockManager().getHandler(it.block.key().asString())
            it.block = it.block.withHandler(handler)
        }

        eventHandler.addListener(InstanceBlockUpdateEvent::class.java) {
            val block = it.block
            if (block.handler() != null) return@addListener

            val handler = MinecraftServer.getBlockManager().getHandler(block.key().asString()) ?: return@addListener
            it.instance.setBlock(it.blockPosition, block.withHandler(handler), false)
        }
    }
}
