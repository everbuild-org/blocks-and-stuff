package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.blocks.behavior.DropperHandler
import org.everbuild.blocksandstuff.common.utils.handlerOrRegistered
import org.everbuild.blocksandstuff.redstone.UpdateContext

/** Dropper: ejects one item immediately on a rising redstone edge. */
class DropperComponent : RisingEdgeTriggerComponent() {
    override fun activate(ctx: UpdateContext) {
        (ctx.instance.getBlock(ctx.position).handlerOrRegistered() as? DropperHandler)
            ?.eject(ctx.instance, ctx.position)
    }
}
