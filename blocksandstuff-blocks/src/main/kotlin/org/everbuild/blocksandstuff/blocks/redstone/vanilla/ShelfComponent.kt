package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import org.everbuild.blocksandstuff.blocks.behavior.ShelfConnections

/** Shelves mirror the redstone input in their `powered` state. */
class ShelfComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val powered = isPowered(ctx)
        val poweredBlock =
            if ((block.getProperty("powered") == "true") == powered) {
                block
            } else {
                block.withProperty("powered", powered.toString())
            }
        val updated = ShelfConnections.updateSideChain(ctx.instance, ctx.position, poweredBlock)
        if (updated.stateId() == block.stateId()) return ComponentEvaluation.None
        return ComponentEvaluation.SetState(updated)
    }
}
