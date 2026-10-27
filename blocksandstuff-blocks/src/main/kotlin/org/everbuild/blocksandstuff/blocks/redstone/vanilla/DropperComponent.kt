package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.blocks.behavior.DropperHandler
import org.everbuild.blocksandstuff.common.utils.handlerOrRegistered
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/** Dropper: ejects one item immediately on a rising redstone edge. */
class DropperComponent : RedstoneComponent {
    private var lastPowered = false

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val powered = isPoweredIncludingAbove(ctx)
        val block = ctx.block()
        if (powered == lastPowered) return ComponentEvaluation.None

        lastPowered = powered
        if (!powered) return ComponentEvaluation.SetState(block.withProperty("triggered", "false"))

        return ComponentEvaluation.Trigger { context ->
            (context.instance.getBlock(context.position).handlerOrRegistered() as? DropperHandler)
                ?.eject(context.instance, context.position)
            context.setState(context.block().withProperty("triggered", "true"))
        }
    }
}
