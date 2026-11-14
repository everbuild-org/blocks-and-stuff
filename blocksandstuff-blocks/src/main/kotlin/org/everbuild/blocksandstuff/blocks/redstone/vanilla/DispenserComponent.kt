package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.blocks.behavior.DispenserHandler
import org.everbuild.blocksandstuff.common.utils.handlerOrRegistered
import org.everbuild.blocksandstuff.redstone.UpdateContext

/** Dispenser: dispenses one item immediately on a rising redstone edge. */
class DispenserComponent : RisingEdgeTriggerComponent() {
    override fun activate(ctx: UpdateContext) {
        (ctx.instance.getBlock(ctx.position).handlerOrRegistered() as? DispenserHandler)
            ?.dispense(ctx.instance, ctx.position)
    }
}
