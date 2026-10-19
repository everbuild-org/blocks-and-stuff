package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.coordinate.Point
import org.everbuild.blocksandstuff.redstone.ALL_FACES
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Two-part door driven by redstone. Both halves share the `open` state; a manually opened,
 * unpowered door is left untouched (state only changes on an actual power transition).
 */
class DoorComponent : RedstoneComponent {
    private var powered = false

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val half = block.getProperty("half") ?: return ComponentEvaluation.None
        val otherPos = if (half == "lower") ctx.position.add(0.0, 1.0, 0.0) else ctx.position.sub(0.0, 1.0, 0.0)
        val other = ctx.block(otherPos)

        val now = isPowered(ctx, ctx.position) || (other.key() == block.key() && isPowered(ctx, otherPos))
        if (now == powered) return ComponentEvaluation.None
        powered = now

        val open = now.toString()
        if (other.key() == block.key() && other.getProperty("half") != half) {
            ctx.setState(otherPos, other.withProperty("open", open))
        }
        return ComponentEvaluation.SetState(block.withProperty("open", open))
    }

    private fun isPowered(
        ctx: ReadContext,
        position: Point,
    ): Boolean {
        for (face in ALL_FACES) {
            val neighbour = position.relative(face)
            val signal = maxOf(
                ctx.power().weakSignal(neighbour, face.oppositeFace),
                ctx.power().directSignal(neighbour, face.oppositeFace),
            )
            if (signal > 0) return true
        }
        return false
    }
}
