package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.event.instance.InstanceBlockUpdateEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace

/**
 * Removes an extended piston's head when its base is replaced or broken. Minestom does not link the
 * two blocks, so without this an extended piston leaves an orphaned `piston_head` behind.
 */
object PistonCleanupRegistration {
    private var registeredHandler: Any? = null

    @JvmStatic
    fun registerDefault() {
        val handler = MinecraftServer.getGlobalEventHandler()
        if (registeredHandler === handler) return
        registeredHandler = handler

        handler.addListener(InstanceBlockUpdateEvent::class.java) { event ->
            val block = event.block
            if (block.compare(Block.PISTON) || block.compare(Block.STICKY_PISTON) || block.compare(Block.MOVING_PISTON)) {
                return@addListener
            }

            for (face in BlockFace.entries) {
                val headPosition = event.blockPosition.relative(face)
                if (!event.instance.isChunkLoaded(headPosition)) continue
                val head = event.instance.getBlock(headPosition)
                val isHead =
                    head.compare(Block.PISTON_HEAD) ||
                        (head.compare(Block.MOVING_PISTON) && movedState(head)?.compare(Block.PISTON_HEAD) == true)
                if (!isHead) continue

                val facing = facingOf(head) ?: continue
                if (headPosition.relative(facing.oppositeFace) != event.blockPosition) continue
                event.instance.setBlock(headPosition, Block.AIR, false)
            }
        }
    }

    private fun facingOf(block: Block): BlockFace? =
        when (block.getProperty("facing")) {
            "up" -> BlockFace.TOP
            "down" -> BlockFace.BOTTOM
            "north" -> BlockFace.NORTH
            "south" -> BlockFace.SOUTH
            "east" -> BlockFace.EAST
            "west" -> BlockFace.WEST
            else -> null
        }
}
