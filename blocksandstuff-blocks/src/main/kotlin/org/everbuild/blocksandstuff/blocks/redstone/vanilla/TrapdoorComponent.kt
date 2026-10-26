package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Trapdoor driven by redstone. Uses the `powered` block state as source of truth so manually
 * opened wooden trapdoors stay untouched while unpowered, and plays the matching material sound.
 */
class TrapdoorComponent : RedstoneComponent {
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
            Sound.sound(soundEvent(block, opening), Sound.Source.BLOCK, 1.0f, 1.0f),
            ctx.position,
        )
    }

    private fun soundEvent(
        block: Block,
        opening: Boolean,
    ): SoundEvent {
        val key = block.key().value()
        return when {
            key.contains("iron_trapdoor") ->
                if (opening) SoundEvent.BLOCK_IRON_TRAPDOOR_OPEN else SoundEvent.BLOCK_IRON_TRAPDOOR_CLOSE
            key.contains("copper_trapdoor") ->
                if (opening) SoundEvent.BLOCK_COPPER_TRAPDOOR_OPEN else SoundEvent.BLOCK_COPPER_TRAPDOOR_CLOSE
            key.startsWith("bamboo") ->
                if (opening) SoundEvent.BLOCK_BAMBOO_WOOD_TRAPDOOR_OPEN else SoundEvent.BLOCK_BAMBOO_WOOD_TRAPDOOR_CLOSE
            key.startsWith("cherry") ->
                if (opening) SoundEvent.BLOCK_CHERRY_WOOD_TRAPDOOR_OPEN else SoundEvent.BLOCK_CHERRY_WOOD_TRAPDOOR_CLOSE
            key.startsWith("crimson") || key.startsWith("warped") ->
                if (opening) SoundEvent.BLOCK_NETHER_WOOD_TRAPDOOR_OPEN else SoundEvent.BLOCK_NETHER_WOOD_TRAPDOOR_CLOSE
            else ->
                if (opening) SoundEvent.BLOCK_WOODEN_TRAPDOOR_OPEN else SoundEvent.BLOCK_WOODEN_TRAPDOOR_CLOSE
        }
    }
}
