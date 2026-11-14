package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Two-part door driven by redstone. The `powered` block state is the source of truth, so a saved
 * door closes/opens correctly after a reload and manually opened doors stay untouched while
 * unpowered.
 */
class DoorComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val half = block.getProperty("half") ?: return ComponentEvaluation.None
        val otherPos = if (half == "lower") ctx.position.add(0.0, 1.0, 0.0) else ctx.position.sub(0.0, 1.0, 0.0)
        val other = ctx.block(otherPos)

        val nowPowered = isPowered(ctx, ctx.position) || (other.key() == block.key() && isPowered(ctx, otherPos))
        val wasPowered = block.getProperty("powered") == "true"
        if (nowPowered == wasPowered) return ComponentEvaluation.None

        playSound(ctx, block, nowPowered)
        val state = nowPowered.toString()
        if (other.key() == block.key() && other.getProperty("half") != half) {
            ctx.setState(otherPos, other.withProperty("powered", state).withProperty("open", state))
        }
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
            key.contains("iron_door") ->
                if (opening) SoundEvent.BLOCK_IRON_DOOR_OPEN else SoundEvent.BLOCK_IRON_DOOR_CLOSE
            key.contains("copper_door") ->
                if (opening) SoundEvent.BLOCK_COPPER_DOOR_OPEN else SoundEvent.BLOCK_COPPER_DOOR_CLOSE
            key.startsWith("bamboo") ->
                if (opening) SoundEvent.BLOCK_BAMBOO_WOOD_DOOR_OPEN else SoundEvent.BLOCK_BAMBOO_WOOD_DOOR_CLOSE
            key.startsWith("cherry") ->
                if (opening) SoundEvent.BLOCK_CHERRY_WOOD_DOOR_OPEN else SoundEvent.BLOCK_CHERRY_WOOD_DOOR_CLOSE
            key.startsWith("crimson") || key.startsWith("warped") ->
                if (opening) SoundEvent.BLOCK_NETHER_WOOD_DOOR_OPEN else SoundEvent.BLOCK_NETHER_WOOD_DOOR_CLOSE
            else ->
                if (opening) SoundEvent.BLOCK_WOODEN_DOOR_OPEN else SoundEvent.BLOCK_WOODEN_DOOR_CLOSE
        }
    }
}
