package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

internal fun poweredBlockState(ctx: UpdateContext): ComponentEvaluation {
    val block = ctx.block()
    val powered = isPowered(ctx)
    if ((block.getProperty("powered") == "true") == powered) return ComponentEvaluation.None
    return ComponentEvaluation.SetState(block.withProperty("powered", powered.toString()))
}

/** Provides rising-edge trigger state for vanilla redstone components. */
abstract class RisingEdgeTriggerComponent : RedstoneComponent {
    private var lastPowered = false

    final override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val powered = isPoweredIncludingAbove(ctx)
        val block = ctx.block()
        if (powered == lastPowered) return ComponentEvaluation.None

        lastPowered = powered
        if (!powered) return ComponentEvaluation.SetState(block.withProperty("triggered", "false"))

        return ComponentEvaluation.Trigger { context ->
            activate(context)
            context.setState(context.block().withProperty("triggered", "true"))
        }
    }

    protected abstract fun activate(ctx: UpdateContext)
}
