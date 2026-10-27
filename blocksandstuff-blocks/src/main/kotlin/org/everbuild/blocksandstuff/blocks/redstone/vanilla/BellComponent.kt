package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Bell. Rings on a rising redstone edge and mirrors the input in the `powered` state.
 */
class BellComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val powered = isPowered(ctx)
        val wasPowered = block.getProperty("powered") == "true"
        if (powered == wasPowered) return ComponentEvaluation.None

        if (powered) ringBell(ctx.instance, ctx.position, Facing.of(block) ?: BlockFace.NORTH)
        return ComponentEvaluation.SetState(block.withProperty("powered", powered.toString()))
    }
}
