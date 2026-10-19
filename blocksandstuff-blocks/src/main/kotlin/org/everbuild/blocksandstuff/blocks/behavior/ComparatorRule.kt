package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

/** Right-clicking a comparator toggles between compare and subtract mode, like vanilla. */
val COMPARATOR_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:comparator") { ctx ->
        val mode = ctx.block.getProperty("mode") ?: "compare"
        val next = if (mode == "compare") "subtract" else "compare"

        ctx.replaceBlock(ctx.block.withProperty("mode", next))
        ctx.instance.playSoundExcept(
            ctx.player,
            Sound.sound(SoundEvent.BLOCK_STONE_BUTTON_CLICK_ON, Sound.Source.BLOCK, 0.3f, 1.2f),
            ctx.blockPosition,
        )

        RuleResult.Consume
    }

fun BlockRulesetEditor.addComparators() {
    blocks(Block.COMPARATOR) { interact.append { install(COMPARATOR_RULE) } }
}
