package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ReadContext

internal object Facing {
    fun of(block: Block): BlockFace? = when (block.getProperty("facing")) {
        "north" -> BlockFace.NORTH
        "south" -> BlockFace.SOUTH
        "east" -> BlockFace.EAST
        "west" -> BlockFace.WEST
        "up" -> BlockFace.TOP
        "down" -> BlockFace.BOTTOM
        else -> null
    }

    fun sides(facing: BlockFace): List<BlockFace> =
        BlockFace.entries.filter { it.toDirection().horizontal() && it != facing && it != facing.oppositeFace }
}

/** Whether any side delivers a weak or direct signal into [position] (default: `ctx.position`). */
internal fun isPowered(
    ctx: ReadContext,
    position: Point = ctx.position,
): Boolean =
    BlockFace.entries.any { face ->
        val neighbour = position.relative(face)
        maxOf(
            ctx.power().weakSignal(neighbour, face.oppositeFace),
            ctx.power().directSignal(neighbour, face.oppositeFace),
        ) > 0
    }

/** Power arriving into `ctx.position` from the neighbour at [neighbourFace]. */
internal fun inputFrom(
    ctx: ReadContext,
    neighbourFace: BlockFace,
    directionFromNeighbour: BlockFace,
): Int {
    val neighbour = ctx.position.relative(neighbourFace)
    val block = ctx.block(neighbour)
    return if (WireComponent.isWire(block)) {
        WireComponent.powerOf(block)
    } else {
        maxOf(
            ctx.power().weakSignal(neighbour, directionFromNeighbour),
            ctx.power().directSignal(neighbour, directionFromNeighbour),
        )
    }
}
