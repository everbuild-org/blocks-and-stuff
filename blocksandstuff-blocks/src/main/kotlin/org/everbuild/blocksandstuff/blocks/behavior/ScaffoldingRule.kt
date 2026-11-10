package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.PlacementActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.place

val SCAFFOLDING_PLACE_RULE: Rule<PlacementActivityContext> =
    Rule("blocksandstuff:scaffolding_place") { ctx ->
        if (!ctx.previousBlock.isWater()) return@Rule RuleResult.Next

        val current = ctx.instance.getBlock(ctx.blockPosition)
        if (current.getProperty("waterlogged") == "true") return@Rule RuleResult.Next

        ctx.replaceBlock(current.withProperty("waterlogged", "true"))
        RuleResult.Next
    }

fun BlockRulesetEditor.addScaffolding() {
    blocks(Block.SCAFFOLDING) {
        place.append { install(SCAFFOLDING_PLACE_RULE) }
    }
}
