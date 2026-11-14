package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

/** Right-clicking a daylight detector toggles its inverted mode, like vanilla. */
val DAYLIGHT_DETECTOR_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:daylight_detector") { ctx ->
        val inverted = ctx.block.getProperty("inverted") == "true"
        ctx.replaceBlock(ctx.block.withProperty("inverted", (!inverted).toString()))
        RuleResult.Consume
    }

fun BlockRulesetEditor.addDaylightDetectors() {
    blocks(Block.DAYLIGHT_DETECTOR) { interact.append { install(DAYLIGHT_DETECTOR_RULE) } }
}
