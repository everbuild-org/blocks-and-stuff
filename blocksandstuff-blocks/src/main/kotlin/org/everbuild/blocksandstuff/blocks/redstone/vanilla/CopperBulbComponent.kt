package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Copper bulb. On a rising redstone edge it toggles `lit` and mirrors the input in `powered`.
 * Falling edges only clear `powered`, leaving `lit` unchanged, exactly like vanilla.
 */
class CopperBulbComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val powered = isPowered(ctx)
        val wasPowered = block.getProperty("powered") == "true"
        if (powered == wasPowered) return ComponentEvaluation.None

        val lit = block.getProperty("lit") == "true"
        val updated =
            if (powered) {
                val nextLit = !lit
                playSound(ctx, if (nextLit) SoundEvent.BLOCK_COPPER_BULB_TURN_ON else SoundEvent.BLOCK_COPPER_BULB_TURN_OFF)
                block.withProperty("powered", "true").withProperty("lit", nextLit.toString())
            } else {
                block.withProperty("powered", "false")
            }
        return ComponentEvaluation.SetState(updated)
    }

    private fun playSound(
        ctx: UpdateContext,
        event: SoundEvent,
    ) {
        ctx.instance.playSoundExcept(
            null,
            Sound.sound(event, Sound.Source.BLOCK, 1.0f, 1.0f),
            ctx.position,
        )
    }
}
