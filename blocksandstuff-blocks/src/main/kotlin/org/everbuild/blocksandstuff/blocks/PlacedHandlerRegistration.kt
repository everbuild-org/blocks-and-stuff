package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerBlockPlaceEvent

object PlacedHandlerRegistration {
    @JvmStatic
    fun registerDefault() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockPlaceEvent::class.java) {
            val handler = MinecraftServer.getBlockManager().getHandler(it.block.key().asString())
            it.block = it.block.withHandler(handler)
        }
    }
}