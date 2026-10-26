package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.blocks.behavior.DispenserHandler
import org.everbuild.blocksandstuff.common.utils.handlerOrRegistered
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/** Dispenser: dispenses one item four game ticks after a rising redstone edge. */
class DispenserComponent : RedstoneComponent {
    private var lastPowered = false
    private var triggerAt = -1L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val powered = isPoweredIncludingAbove(ctx)
        val now = ctx.tick()
        val block = ctx.block()

        if (triggerAt >= 0) {
            if (powered != lastPowered) {
                lastPowered = powered
                if (!powered) return ComponentEvaluation.SetState(block.withProperty("triggered", "false"))
                triggerAt = now + DELAY_TICKS
                ctx.scheduleTick(DELAY_TICKS)
                return ComponentEvaluation.None
            }
            if (now < triggerAt) return ComponentEvaluation.None
            triggerAt = -1L
            if (!powered) return ComponentEvaluation.SetState(block.withProperty("triggered", "false"))
            return ComponentEvaluation.Trigger { context ->
                (context.instance.getBlock(context.position).handlerOrRegistered() as? DispenserHandler)
                    ?.dispense(context.instance, context.position)
                context.setState(context.block().withProperty("triggered", "true"))
            }
        }

        if (powered == lastPowered) return ComponentEvaluation.None
        lastPowered = powered
        if (!powered) return ComponentEvaluation.SetState(block.withProperty("triggered", "false"))

        triggerAt = now + DELAY_TICKS
        ctx.scheduleTick(DELAY_TICKS)
        return ComponentEvaluation.SetState(block.withProperty("triggered", "true"))
    }

    private companion object {
        private const val DELAY_TICKS = 4
    }
}
