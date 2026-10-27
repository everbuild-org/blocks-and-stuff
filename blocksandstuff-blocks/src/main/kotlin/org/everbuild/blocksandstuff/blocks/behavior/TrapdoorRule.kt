package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.redstone.VibrationBus
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

internal val trapdoorBlocks: Collection<Block> =
    BlockTags
        .getTaggedWith("minecraft:trapdoors")
        .filter { it.key().asString() != "minecraft:iron_trapdoor" }

val TRAPDOOR_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:trapdoor") { ctx ->
        if (ctx.player.isSneaking && !ctx.player.itemInMainHand.isAir) return@Rule RuleResult.Next

        val newOpen = (ctx.block.getProperty("open") != "true").toString()
        ctx.replaceBlock(ctx.block.withProperty("open", newOpen))
        VibrationBus.emit(ctx.instance, ctx.blockPosition, if (newOpen == "true") 10 else 9)

        ctx.instance.playSoundExcept(
            ctx.player,
            Sound.sound(trapdoorSound(ctx.block, newOpen == "true"), Sound.Source.BLOCK, 1.0f, 1.0f),
            ctx.blockPosition,
        )

        RuleResult.Consume
    }

private fun trapdoorSound(
    block: Block,
    open: Boolean,
): SoundEvent {
    val name = block.key().value()
    return when {
        name.startsWith("bamboo") ->
            if (open) SoundEvent.BLOCK_BAMBOO_WOOD_TRAPDOOR_OPEN else SoundEvent.BLOCK_BAMBOO_WOOD_TRAPDOOR_CLOSE
        name.startsWith("cherry") ->
            if (open) SoundEvent.BLOCK_CHERRY_WOOD_TRAPDOOR_OPEN else SoundEvent.BLOCK_CHERRY_WOOD_TRAPDOOR_CLOSE
        name.startsWith("crimson") || name.startsWith("warped") ->
            if (open) SoundEvent.BLOCK_NETHER_WOOD_TRAPDOOR_OPEN else SoundEvent.BLOCK_NETHER_WOOD_TRAPDOOR_CLOSE
        name.contains("copper") ->
            if (open) SoundEvent.BLOCK_COPPER_TRAPDOOR_OPEN else SoundEvent.BLOCK_COPPER_TRAPDOOR_CLOSE
        else -> if (open) SoundEvent.BLOCK_WOODEN_TRAPDOOR_OPEN else SoundEvent.BLOCK_WOODEN_TRAPDOOR_CLOSE
    }
}

fun BlockRulesetEditor.addTrapdoors() {
    trapdoorBlocks.forEach { block ->
        if (isWaxableCopper(block)) {
            blocks(block) { interact.append { install(TRAPDOOR_RULE) } }
        } else {
            blocks(block) { interact.append { install(TRAPDOOR_RULE) } }
        }
    }
}
