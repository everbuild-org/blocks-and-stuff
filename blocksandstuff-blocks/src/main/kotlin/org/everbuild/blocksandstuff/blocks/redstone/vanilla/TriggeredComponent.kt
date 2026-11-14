package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Dispenser / dropper: reflects power in the `triggered` block state. Actual item dispensing is not
 * modelled yet (would need block-entity inventory access).
 */
class TriggeredComponent : RedstoneComponent {
    private var lastPowered = false

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val powered = isPowered(ctx)
        if (powered == lastPowered) return ComponentEvaluation.None
        lastPowered = powered
        return ComponentEvaluation.SetState(ctx.block().withProperty("triggered", powered.toString()))
    }
}
