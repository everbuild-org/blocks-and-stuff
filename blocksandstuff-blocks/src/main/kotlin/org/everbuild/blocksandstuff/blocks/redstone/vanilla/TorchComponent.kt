package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ALL_FACES
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Redstone torch (floor and wall). Lit unless the block it is mounted on is powered by another
 * source; the torch's own contribution to that block is ignored.
 */
class TorchComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val support = supportOf(block, ctx.position) ?: return ComponentEvaluation.None
        val lit = !isPowered(ctx, support, ctx.position)
        if ((block.getProperty("lit") == "true") == lit) return ComponentEvaluation.None
        return ComponentEvaluation.SetState(block.withProperty("lit", lit.toString()))
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit = if (ctx.block().getProperty("lit") == "true") Emit(REDSTONE_MAX_POWER, REDSTONE_MAX_POWER) else Emit.ZERO

    private fun isPowered(
        ctx: ReadContext,
        position: Point,
        exclude: Point,
    ): Boolean {
        for (face in ALL_FACES) {
            val neighbour = position.relative(face)
            if (neighbour == exclude) continue
            val block = ctx.block(neighbour)
            val into = face.oppositeFace
            val signal =
                if (WireComponent.isWire(block)) {
                    ctx.power().weakSignal(neighbour, into)
                } else {
                    maxOf(ctx.power().weakSignal(neighbour, into), ctx.power().directSignal(neighbour, into))
                }
            if (signal > 0) return true
        }
        return false
    }

    companion object {
        fun supportOf(
            block: Block,
            position: Point,
        ): Point? = when {
            block.compare(Block.REDSTONE_WALL_TORCH) -> {
                val facing = when (block.getProperty("facing")) {
                    "north" -> BlockFace.NORTH
                    "south" -> BlockFace.SOUTH
                    "east" -> BlockFace.EAST
                    "west" -> BlockFace.WEST
                    else -> null
                }
                facing?.let { position.relative(it) }
            }

            block.compare(Block.REDSTONE_TORCH) -> position.relative(BlockFace.BOTTOM)
            else -> null
        }
    }
}
