package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.sound.SoundEvent
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

        if (powered) {
            ctx.instance.playSoundExcept(
                null,
                Sound.sound(SoundEvent.BLOCK_BELL_USE, Sound.Source.BLOCK, 2.0f, 1.0f),
                ctx.position,
            )
        }
        return ComponentEvaluation.SetState(block.withProperty("powered", powered.toString()))
    }
}
