package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Redstone lamp. Lights up immediately while powered and turns off four game ticks after the power
 * is removed, like vanilla.
 */
class LampComponent : RedstoneComponent {
    private var tickScheduled = false
    private var scheduledAt = 0L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val lit = block.getProperty("lit") == "true"
        val powered = isPowered(ctx)
        val now = ctx.tick()

        if (tickScheduled && now >= scheduledAt) {
            tickScheduled = false
            if (powered) return ComponentEvaluation.None
            return ComponentEvaluation.SetState(block.withProperty("lit", "false"))
        }

        if (powered) {
            tickScheduled = false
            return if (lit) ComponentEvaluation.None else ComponentEvaluation.SetState(block.withProperty("lit", "true"))
        }

        if (!lit || tickScheduled) return ComponentEvaluation.None

        tickScheduled = true
        scheduledAt = now + OFF_DELAY_TICKS
        ctx.scheduleTick(OFF_DELAY_TICKS)
        return ComponentEvaluation.None
    }

    private companion object {
        /** Vanilla redstone lamp delay: two redstone ticks (four game ticks). */
        private const val OFF_DELAY_TICKS = 4
    }
}
