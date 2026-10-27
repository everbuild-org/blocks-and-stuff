@file:Suppress("UnstableApiUsage")

package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import net.minestom.server.event.instance.InstanceBlockUpdateEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.instance.block.BlockHandler

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
            val existing = block.handler()
            if (existing != null && existing !is BlockHandler.Dummy) return@addListener

            val handler = MinecraftServer.getBlockManager().getHandler(block.key().asString()) ?: return@addListener
            it.instance.setBlock(it.blockPosition, block.withHandler(handler), false)
        }

        eventHandler.addListener(PlayerBlockInteractEvent::class.java) { event ->
            val block = event.block
            val existing = block.handler()
            if (existing != null && existing !is BlockHandler.Dummy) return@addListener

            val handler = MinecraftServer.getBlockManager().getHandler(block.key().asString()) ?: return@addListener
            event.instance.setBlock(event.blockPosition, block.withHandler(handler), false)
            val handled =
                !handler.onInteract(
                    BlockHandler.Interaction(
                        block.withHandler(handler),
                        event.instance,
                        event.blockFace,
                        event.blockPosition,
                        event.cursorPosition,
                        event.player,
                        event.hand,
                    ),
                )
            if (handled) event.isBlockingItemUse = true
        }
    }
}
