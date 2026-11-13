package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/** Shelves mirror the redstone input in their `powered` state. */
class ShelfComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val powered = isPowered(ctx)
        if ((block.getProperty("powered") == "true") == powered) return ComponentEvaluation.None
        return ComponentEvaluation.SetState(block.withProperty("powered", powered.toString()))
    }
}
