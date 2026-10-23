package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.blocks.behavior.DispenserHandler
import org.everbuild.blocksandstuff.common.utils.handlerOrRegistered
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/** Dispenser: dispenses one item on a rising redstone edge and mirrors power in `triggered`. */
class DispenserComponent : RedstoneComponent {
    private var lastPowered = false

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val powered = isPowered(ctx)
        if (powered == lastPowered) return ComponentEvaluation.None
        lastPowered = powered

        return ComponentEvaluation.Trigger { context ->
            if (powered) {
                (context.instance.getBlock(context.position).handlerOrRegistered() as? DispenserHandler)
                    ?.dispense(context.instance, context.position)
            }
            context.setState(context.block().withProperty("triggered", powered.toString()))
        }
    }
}
