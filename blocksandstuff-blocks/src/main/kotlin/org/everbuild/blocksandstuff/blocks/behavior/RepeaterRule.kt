package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

/**
 * Right-clicking a repeater cycles its delay between 1 and 4 redstone ticks, like vanilla.
 */
val REPEATER_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:repeater") { ctx ->
        val delay = ctx.block.getProperty("delay")?.toIntOrNull() ?: 1
        val next = if (delay >= 4) 1 else delay + 1

        ctx.replaceBlock(ctx.block.withProperty("delay", next.toString()))
        ctx.instance.playSoundExcept(
            ctx.player,
            Sound.sound(SoundEvent.BLOCK_STONE_BUTTON_CLICK_ON, Sound.Source.BLOCK, 0.3f, 1.0f),
            ctx.blockPosition,
        )

        RuleResult.Consume
    }

fun BlockRulesetEditor.addRepeaters() {
    blocks(Block.REPEATER) { interact.append { install(REPEATER_RULE) } }
}
