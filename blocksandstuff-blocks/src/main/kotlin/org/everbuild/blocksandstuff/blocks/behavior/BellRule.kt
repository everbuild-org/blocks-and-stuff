package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.redstone.vanilla.ringBell

/** Rings a bell on right-click, independent of the per-block handler attachment. */
object BellInteraction {
    private var registeredHandler: Any? = null

    @JvmStatic
    fun enable() {
        val eventHandler = MinecraftServer.getGlobalEventHandler()
        if (registeredHandler === eventHandler) return
        registeredHandler = eventHandler

        eventHandler.addListener(PlayerBlockInteractEvent::class.java) { event ->
            if (!event.block.compare(Block.BELL) || event.isCancelled) return@addListener

            ringBell(event.instance, event.blockPosition, event.blockFace)
            event.isBlockingItemUse = true
        }
    }
}
