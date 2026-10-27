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
 * Observer. Emits a one-tick pulse through its back when the watched block (the one it faces)
 * changes state.
 */
class ObserverComponent : RedstoneComponent {
    private var lastWatched: Block? = null
    private var tickScheduled = false
    private var scheduledAt = 0L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return ComponentEvaluation.None
        val watched = ctx.block(ctx.position.relative(facing))
        val now = ctx.tick()
        val powered = block.getProperty("powered") == "true"

        if (tickScheduled && now >= scheduledAt) {
            tickScheduled = false
            if (powered) {
                lastWatched = watched
                return ComponentEvaluation.SetState(block.withProperty("powered", "false"))
            }
            scheduleTick(ctx, now)
            return ComponentEvaluation.SetState(block.withProperty("powered", "true"))
        }

        if (powered) {
            // A loaded observer with no scheduled end tick is cleared, like vanilla's onPlace.
            lastWatched = watched
            if (!tickScheduled) return ComponentEvaluation.SetState(block.withProperty("powered", "false"))
            return ComponentEvaluation.None
        }

        if (tickScheduled) return ComponentEvaluation.None

        val previous = lastWatched
        lastWatched = watched
        if (previous != null && changed(previous, watched)) scheduleTick(ctx, now)
        return ComponentEvaluation.None
    }

    // <@AI_UNREVIEWED>
    override fun onMovementCompleted(ctx: UpdateContext) {
        if (tickScheduled || ctx.block().getProperty("powered") == "true") return
        scheduleTick(ctx, ctx.tick())
    }
    // </<@AI_UNREVIEWED>

    private fun scheduleTick(
        ctx: UpdateContext,
        now: Long,
    ) {
        tickScheduled = true
        scheduledAt = now + DELAY_TICKS
        ctx.scheduleTick(DELAY_TICKS)
    }

    private companion object {
        /** Vanilla observer delay: one redstone tick (two game ticks). */
        private const val DELAY_TICKS = 2
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

    private fun changed(
        a: Block,
        b: Block,
    ): Boolean = a.stateId() != b.stateId() || a.nbt() != b.nbt()
}
