package org.everbuild.blocksandstuff.blocks.placement

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.tag.BlockTags

/**
 * Computes the connection properties of a redstone wire block. Shared by the placement rule (direct
 * neighbour updates) and the engine component (so diagonal "staircase" wires are refreshed too).
 */
object WireConnections {
    enum class ConnectionType {
        NONE,
        DIRECT,
        UP,
        DOWN,
    }

    private val horizontalFaces = BlockFace.entries.filter { it.toDirection().horizontal() }

    // Matched by block key (not state) so powered components (lit lamp, open door, ...) still connect.
    private val simpleConnectionKeys: Set<Key> =
        (
            listOf(
                Block.REDSTONE_BLOCK,
                Block.REDSTONE_LAMP,
                Block.REDSTONE_TORCH,
                Block.REDSTONE_WALL_TORCH,
                Block.REPEATER,
                Block.COMPARATOR,
                Block.OBSERVER,
                Block.DISPENSER,
                Block.DROPPER,
                Block.HOPPER,
                Block.NOTE_BLOCK,
                Block.PISTON,
                Block.STICKY_PISTON,
                Block.RAIL,
                Block.POWERED_RAIL,
                Block.ACTIVATOR_RAIL,
                Block.DETECTOR_RAIL,
                Block.CALIBRATED_SCULK_SENSOR,
                Block.SCULK_SENSOR,
                Block.DAYLIGHT_DETECTOR,
                Block.JUKEBOX,
                Block.LEVER,
                Block.TARGET,
                Block.TRAPPED_CHEST,
            ) +
                BlockTags.getTaggedWith("minecraft:buttons") +
                BlockTags.getTaggedWith("minecraft:pressure_plates") +
                BlockTags.getTaggedWith("minecraft:doors") +
                BlockTags.getTaggedWith("minecraft:trapdoors") +
                BlockTags.getTaggedWith("minecraft:fence_gates")
        ).map { it.key() }.toSet()

    /** Returns the wire block with connection properties applied, or `null` if it is unsupported. */
    fun updatedWire(
        instance: Block.Getter,
        position: Point,
        current: Block,
    ): Block? {
        val bottomBlock = instance.getBlock(position.sub(0.0, 1.0, 0.0))
        val supported = bottomBlock?.collisionShape()?.isFaceFull(BlockFace.TOP) ?: false
        if (!supported) return null

        val sideConnections = getSideConnections(instance, position, true)
        val bottomSideConnections = getSideConnections(instance, position.sub(0.0, 1.0, 0.0), false)
        val topSideConnections = getSideConnections(instance, position.add(0.0, 1.0, 0.0), false)

        val connections =
            horizontalFaces.associate { face ->
                when {
                    // Vanilla priority: a same-level neighbour wins over an up/down staircase.
                    face in sideConnections -> face to ConnectionType.DIRECT

                    face in topSideConnections -> face to ConnectionType.UP

                    face in bottomSideConnections -> face to ConnectionType.DOWN

                    else -> face to ConnectionType.NONE
                }
            }

        val preliminary =
            connections.entries.fold(current) { acc, (face, connection) ->
                return@fold acc.withProperty(
                    face.name.lowercase(),
                    when (connection) {
                        ConnectionType.NONE -> "none"
                        ConnectionType.DIRECT -> "side"
                        ConnectionType.UP -> "up"
                        ConnectionType.DOWN -> "side"
                    },
                )
            }

        val actual = connections.filter { it.value != ConnectionType.NONE }
        if (actual.isEmpty()) {
            return current.withProperties(horizontalFaces.associate { it.name.lowercase() to "side" })
        }
        if (actual.size == 1) {
            val (face, _) = actual.entries.first()
            return preliminary.withProperty(face.oppositeFace.name.lowercase(), "side")
        }
        return preliminary
    }

    /** Positions of connected wire blocks that can feed power into [position], including staircases. */
    fun connectedWirePositions(
        instance: Block.Getter,
        position: Point,
    ): Set<Point> {
        val sideConnections = getSideConnections(instance, position, true)
        val bottomSideConnections = getSideConnections(instance, position.sub(0.0, 1.0, 0.0), false)
        val topSideConnections = getSideConnections(instance, position.add(0.0, 1.0, 0.0), false)

        return horizontalFaces
            .mapNotNull { face ->
                when {
                    face in sideConnections -> position.relative(face).takeIf { isWire(instance, it) }
                    face in topSideConnections -> position.relative(face).add(0.0, 1.0, 0.0)
                    face in bottomSideConnections -> position.relative(face).sub(0.0, 1.0, 0.0)
                    else -> null
                }
            }.toSet()
    }

    private fun isWire(
        instance: Block.Getter,
        position: Point,
    ): Boolean = instance.getBlock(position).compare(Block.REDSTONE_WIRE)

    private fun getSideConnections(
        instance: Block.Getter,
        position: Point,
        direct: Boolean,
    ): List<BlockFace> =
        horizontalFaces.filter { face ->
            val block = instance.getBlock(position.relative(face))
            (direct && block.key() in simpleConnectionKeys) || block.compare(Block.REDSTONE_WIRE)
        }
}
