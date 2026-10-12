package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val fenceGates: Collection<Block> = BlockTags.getTaggedWith("minecraft:fence_gates")

val FENCE_GATE_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:fence_gate") { ctx ->
        if (ctx.player.isSneaking && !ctx.player.itemInMainHand.isAir) return@Rule RuleResult.Next

        val block = ctx.block
        val newOpen = block.getProperty("open") != "true"

        var updated = block.withProperty("open", newOpen.toString())
        if (newOpen) {
            val playerDirection = BlockFace.fromYaw(ctx.player.position.yaw).toDirection()
            val facing = Direction.valueOf(block.getProperty("facing")!!.uppercase())
            if (facing == playerDirection.opposite()) {
                updated = updated.withProperty("facing", playerDirection.name.lowercase())
            }
        }

        ctx.replaceBlock(updated)

        ctx.instance.playSoundExcept(
            ctx.player,
            Sound.sound(fenceGateSound(block, newOpen), Sound.Source.BLOCK, 1.0f, 1.0f),
            ctx.blockPosition,
        )

        RuleResult.Consume
    }

private fun fenceGateSound(
    block: Block,
    open: Boolean,
): SoundEvent {
    val name = block.key().value()
    return when {
        name.startsWith("bamboo") -> {
            if (open) SoundEvent.BLOCK_BAMBOO_WOOD_FENCE_GATE_OPEN else SoundEvent.BLOCK_BAMBOO_WOOD_FENCE_GATE_CLOSE
        }

        name.startsWith("cherry") -> {
            if (open) SoundEvent.BLOCK_CHERRY_WOOD_FENCE_GATE_OPEN else SoundEvent.BLOCK_CHERRY_WOOD_FENCE_GATE_CLOSE
        }

        name.startsWith("crimson") || name.startsWith("warped") -> {
            if (open) SoundEvent.BLOCK_NETHER_WOOD_FENCE_GATE_OPEN else SoundEvent.BLOCK_NETHER_WOOD_FENCE_GATE_CLOSE
        }

        else -> {
            if (open) SoundEvent.BLOCK_FENCE_GATE_OPEN else SoundEvent.BLOCK_FENCE_GATE_CLOSE
        }
    }
}

fun BlockRulesetEditor.addFenceGates() {
    fenceGates.forEach { block ->
        blocks(block) { interact.append { install(FENCE_GATE_RULE) } }
    }
}
