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
            installHandler(it.instance, it.blockPosition, it.block)
        }

        eventHandler.addListener(PlayerBlockInteractEvent::class.java) { event ->
            val block = event.block
            val handler = installHandler(event.instance, event.blockPosition, block) ?: return@addListener
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

    private fun installHandler(
        instance: net.minestom.server.instance.Instance,
        position: net.minestom.server.coordinate.Point,
        block: net.minestom.server.instance.block.Block,
    ): BlockHandler? {
        val existing = block.handler()
        if (existing != null && existing !is BlockHandler.Dummy) return null

        val handler = MinecraftServer.getBlockManager().getHandler(block.key().asString()) ?: return null
        instance.setBlock(position, block.withHandler(handler), false)
        return handler
    }
}
