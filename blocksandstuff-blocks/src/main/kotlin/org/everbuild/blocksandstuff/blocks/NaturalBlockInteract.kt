package org.everbuild.blocksandstuff.blocks

import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.block.BlockHandler

/**
 * Minestom only calls [BlockHandler.onInteract] when the interacted block already has a handler
 * instance attached, which is not the case for blocks created by world generation. This listener
 * lazily attaches the registered handler on first interaction so block behaviours (hoeing,
 * containers, ...) also work on naturally generated blocks.
 */
object NaturalBlockInteract {
    private val noHandler =
        object : BlockHandler {
            override fun getKey(): Key = Key.key("blocksandstuff:no_handler")
        }
    private val handlers = ConcurrentHashMap<String, BlockHandler>()

    @JvmStatic
    fun enable() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockInteractEvent::class.java) { event ->
            val block = event.block
            if (block.handler() != null) return@addListener

            val key = block.key().asString()
            val handler = handlers.computeIfAbsent(key) { MinecraftServer.getBlockManager().getHandler(key) ?: noHandler }
            if (handler === noHandler) return@addListener

            event.instance.setBlock(event.blockPosition, block.withHandler(handler), false)
            val handled =
                handler.onInteract(
                    BlockHandler.Interaction(
                        block,
                        event.instance,
                        event.blockFace,
                        event.blockPosition,
                        event.cursorPosition,
                        event.player,
                        event.hand,
                    ),
                )
            if (!handled) event.isBlockingItemUse = true
        }
    }
}
