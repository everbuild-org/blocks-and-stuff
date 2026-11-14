package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.utils.blockFacing
import org.everbuild.blocksandstuff.redstone.ReadContext

internal object Facing {
    fun of(block: Block): BlockFace? = blockFacing(block)

    fun sides(facing: BlockFace): List<BlockFace> =
        BlockFace.entries.filter { it.toDirection().horizontal() && it != facing && it != facing.oppositeFace }
}

/** Whether any side delivers a weak or direct signal into [position] (default: `ctx.position`). */
internal fun isPowered(
    ctx: ReadContext,
    position: Point = ctx.position,
): Boolean =
    BlockFace.entries.any { face ->
        signalFrom(ctx, position.relative(face), face.oppositeFace) > 0
    }

/** Dispensers and droppers are also triggered by power at the block above (quasi-connectivity). */
internal fun isPoweredIncludingAbove(ctx: ReadContext): Boolean = isPowered(ctx) || isPowered(ctx, ctx.position.add(0.0, 1.0, 0.0))

/** Power arriving into `ctx.position` from the neighbour at [neighbourFace]. */
internal fun inputFrom(
    ctx: ReadContext,
    neighbourFace: BlockFace,
    directionFromNeighbour: BlockFace,
): Int = signalFrom(ctx, ctx.position.relative(neighbourFace), directionFromNeighbour)

/**
 * Signal that [neighbour] delivers into the current block. A conductive block without its own
 * component is treated as a powered block, so both strongly and weakly powered blocks can feed
 * repeaters, comparators and mechanisms.
 */
private fun signalFrom(
    ctx: ReadContext,
    neighbour: Point,
    directionFromNeighbour: BlockFace,
): Int {
    val block = ctx.block(neighbour)
    if (ctx.componentAt(neighbour) == null && block.redstoneConductor()) {
        return ctx.power().signalInto(neighbour)
    }
    return maxOf(
        ctx.power().weakSignal(neighbour, directionFromNeighbour),
        ctx.power().directSignal(neighbour, directionFromNeighbour),
    )
}
