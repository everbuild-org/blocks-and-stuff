package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Redstone repeater. Input from the back, output 15 through the front, delayed by `delay` redstone
 * ticks (2 game ticks each). Locked repeaters/comparators on the sides freeze the state.
 */
class RepeaterComponent : RedstoneComponent {
    private var pending: Boolean? = null
    private var pendingAt = 0L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return ComponentEvaluation.None
        val current = block.getProperty("powered") == "true"

        if (isLocked(ctx, facing)) {
            pending = null
            return ComponentEvaluation.None
        }

        // A diode's `facing` points at its input side; the output is the opposite face.
        val desired = inputFrom(ctx, facing, facing.oppositeFace) > 0
        if (desired == current) {
            pending = null
            return ComponentEvaluation.None
        }

        val delay = delayTicks(ctx)
        if (pending != desired) {
            pending = desired
            pendingAt = ctx.tick() + delay
            ctx.scheduleTick(delay)
            return ComponentEvaluation.None
        }

        if (ctx.tick() < pendingAt) return ComponentEvaluation.None

        pending = null
        return ComponentEvaluation.SetState(block.withProperty("powered", desired.toString()))
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit =
        if (Facing.of(ctx.block())?.oppositeFace == face && ctx.block().getProperty("powered") == "true") {
            Emit(REDSTONE_MAX_POWER, REDSTONE_MAX_POWER)
        } else {
            Emit.ZERO
        }

    override fun delayTicks(ctx: ReadContext): Int =
        (ctx.block().getProperty("delay")?.toIntOrNull() ?: 1).coerceIn(1, 4) * 2

    private fun isLocked(
        ctx: ReadContext,
        facing: BlockFace,
    ): Boolean {
        for (side in Facing.sides(facing)) {
            val neighbour = ctx.block(ctx.position.relative(side))
            if (!neighbour.compare(Block.REPEATER) && !neighbour.compare(Block.COMPARATOR)) continue
            if (neighbour.getProperty("powered") != "true") continue

            // A side diode only locks this repeater if it actually outputs into it, i.e. its own
            // facing (input side) points at this repeater. Parallel repeaters must not lock.
            if (Facing.of(neighbour) == side) return true
        }
        return false
    }
}
