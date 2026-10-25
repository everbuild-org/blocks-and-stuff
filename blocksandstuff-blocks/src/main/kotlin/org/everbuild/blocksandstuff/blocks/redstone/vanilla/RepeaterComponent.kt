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
    private var tickScheduled = false
    private var scheduledAt = 0L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return ComponentEvaluation.None
        val now = ctx.tick()
        val locked = isLocked(ctx, facing)
        val lockedBlock = block.withProperty("locked", locked.toString())
        val lockChanged = lockedBlock.stateId() != block.stateId()

        if (tickScheduled && now >= scheduledAt) {
            tickScheduled = false
            if (locked) return if (lockChanged) ComponentEvaluation.SetState(lockedBlock) else ComponentEvaluation.None

            val powered = block.getProperty("powered") == "true"
            val input = hasInput(ctx, facing)
            if (powered && !input) {
                return ComponentEvaluation.SetState(lockedBlock.withProperty("powered", "false"))
            }
            if (!powered) {
                // A short pulse is stretched: if the input is already gone when the delay expires,
                // schedule the falling edge a full delay later.
                if (!input) scheduleTick(ctx, now)
                return ComponentEvaluation.SetState(lockedBlock.withProperty("powered", "true"))
            }
            return if (lockChanged) ComponentEvaluation.SetState(lockedBlock) else ComponentEvaluation.None
        }

        if (!locked && !tickScheduled) {
            val powered = block.getProperty("powered") == "true"
            if (powered != hasInput(ctx, facing)) scheduleTick(ctx, now)
        }
        return if (lockChanged) ComponentEvaluation.SetState(lockedBlock) else ComponentEvaluation.None
    }

    private fun hasInput(
        ctx: UpdateContext,
        facing: BlockFace,
    ): Boolean {
        // A diode's `facing` points at its input side; the output is the opposite face.
        return inputFrom(ctx, facing, facing.oppositeFace) > 0
    }

    private fun scheduleTick(
        ctx: UpdateContext,
        now: Long,
    ) {
        val delay = delayTicks(ctx)
        tickScheduled = true
        scheduledAt = now + delay
        ctx.scheduleTick(delay)
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

    override fun delayTicks(ctx: ReadContext): Int = (ctx.block().getProperty("delay")?.toIntOrNull() ?: 1).coerceIn(1, 4) * 2

    private fun isLocked(
        ctx: ReadContext,
        facing: BlockFace,
    ): Boolean {
        for (side in Facing.sides(facing)) {
            val neighbourPosition = ctx.position.relative(side)
            val neighbour = ctx.block(neighbourPosition)
            if (!neighbour.compare(Block.REPEATER) && !neighbour.compare(Block.COMPARATOR)) continue

            if (ctx.power().directSignal(neighbourPosition, side.oppositeFace) > 0) return true
        }
        return false
    }
}
