package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick

val FARMLAND_RULE: Rule<RandomTickActivityContext> =
    Rule("blocksandstuff:farmland") { ctx ->
        val block = ctx.block
        val updated =
            if (hasNearbyWater(ctx.instance, ctx.blockPosition)) {
                if (block.getProperty("moisture") == "7") null else block.withProperty("moisture", "7")
            } else {
                val moisture = (block.getProperty("moisture")?.toIntOrNull() ?: 0) - 1
                if (moisture < 0) Block.DIRT else block.withProperty("moisture", moisture.toString())
            }

        if (updated == null) return@Rule RuleResult.Next
        ctx.replaceBlock(updated)
        RuleResult.Next
    }

private fun hasNearbyWater(
    instance: Block.Getter,
    pos: Point,
): Boolean {
    for (dy in 0..1) {
        for (dx in -4..4) {
            for (dz in -4..4) {
                if (dx == 0 && dy == 0 && dz == 0) continue
                val bp = pos.add(dx.toDouble(), dy.toDouble(), dz.toDouble())
                if (instance.getBlock(bp).isWater()) return true
            }
        }
    }
    return false
}

fun BlockRulesetEditor.addFarmland() {
    blocks(Block.FARMLAND) { randomTick.append { install(FARMLAND_RULE) } }
}
