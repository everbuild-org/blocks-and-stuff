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

        val sideConnections = getSideConnections(instance, position)
        val bottomSideConnections = getVerticalSideConnections(instance, position, up = false)
        val topSideConnections = getVerticalSideConnections(instance, position, up = true)

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
        val sideConnections = getSideConnections(instance, position)
        val bottomSideConnections = getVerticalSideConnections(instance, position, up = false)
        val topSideConnections = getVerticalSideConnections(instance, position, up = true)

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
    ): List<BlockFace> =
        horizontalFaces.filter { face ->
            val block = instance.getBlock(position.relative(face))
            connectsTo(block, face) || block.compare(Block.REDSTONE_WIRE)
        }

    /**
     * Vanilla cuts a vertical wire connection when the block between the two wire levels conducts
     * redstone (e.g. stone), while air and non-conductive blocks (e.g. glass) allow it. The block
     * between an upper and lower wire depends on the horizontal direction, so this is checked per
     * face.
     */
    private fun getVerticalSideConnections(
        instance: Block.Getter,
        position: Point,
        up: Boolean,
    ): List<BlockFace> =
        horizontalFaces.filter { face ->
            val horizontal = position.relative(face)
            val wirePosition = if (up) horizontal.add(0.0, 1.0, 0.0) else horizontal.sub(0.0, 1.0, 0.0)
            if (!instance.getBlock(wirePosition).compare(Block.REDSTONE_WIRE)) return@filter false

            val between = if (up) position.add(0.0, 1.0, 0.0) else horizontal
            !instance.getBlock(between).redstoneConductor()
        }

    /**
     * Vanilla connection rule: wire attaches to a repeater only along its input/output axis and to
     * an observer only on its output side. Comparators accept side inputs, so wire connects to them
     * from any side. `face` points from the wire to [neighbour].
     */
    private fun connectsTo(
        neighbour: Block,
        face: BlockFace,
    ): Boolean {
        if (neighbour.compare(Block.REPEATER)) {
            val facing = facingOf(neighbour) ?: return false
            return face == facing || face == facing.oppositeFace
        }
        if (neighbour.compare(Block.OBSERVER)) {
            return face == facingOf(neighbour)
        }
        return neighbour.key() in simpleConnectionKeys
    }

    private fun facingOf(block: Block): BlockFace? =
        when (block.getProperty("facing")) {
            "north" -> BlockFace.NORTH
            "south" -> BlockFace.SOUTH
            "east" -> BlockFace.EAST
            "west" -> BlockFace.WEST
            else -> null
        }
}
