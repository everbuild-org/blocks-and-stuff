package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.behavior.fenceGateSound
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Fence gate driven by redstone. The `powered` block state is the source of truth so a manually
 * opened gate stays open while unpowered, and the matching wood sound is played on transitions.
 */
class FenceGateComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val nowPowered = isPowered(ctx, ctx.position)
        val wasPowered = block.getProperty("powered") == "true"
        if (nowPowered == wasPowered) return ComponentEvaluation.None

        playSound(ctx, block, nowPowered)
        val state = nowPowered.toString()
        return ComponentEvaluation.SetState(block.withProperty("powered", state).withProperty("open", state))
    }

    private fun playSound(
        ctx: UpdateContext,
        block: Block,
        opening: Boolean,
    ) {
        ctx.instance.playSoundExcept(
            null,
            Sound.sound(fenceGateSound(block, opening), Sound.Source.BLOCK, 1.0f, 1.0f),
            ctx.position,
        )
    }

}
