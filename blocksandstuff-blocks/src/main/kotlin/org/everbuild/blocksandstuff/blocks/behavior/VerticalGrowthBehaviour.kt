package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.Random

/**
 * Shared random-tick growth for column plants (sugar cane, cactus, bamboo): grows the top block
 * upwards until [maxHeight] is reached.
 */
class VerticalGrowthBehaviour(
    private val maxHeight: Int,
    private val growthChance: Int = 8,
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            val above = ctx.blockPosition.add(0.0, 1.0, 0.0)
            if (!ctx.instance.getBlock(above).air()) return@Rule RuleResult.Next
            if (columnHeight(ctx.instance, ctx.blockPosition, ctx.block) >= maxHeight) return@Rule RuleResult.Next
            if (random.nextInt(growthChance) != 0) return@Rule RuleResult.Next

            ctx.instance.setBlock(above, topState(ctx.block))
            RuleResult.Next
        }

    private fun topState(block: Block): Block = if (block.getProperty("age") != null) block.withProperty("age", "0") else block
}

fun BlockRulesetEditor.addVerticalGrowth() {
    registerColumn(Block.SUGAR_CANE, maxHeight = 3, id = "sugar_cane")
    registerColumn(Block.CACTUS, maxHeight = 3, id = "cactus")
    registerColumn(Block.BAMBOO, maxHeight = 12, id = "bamboo")
}

private fun columnHeight(
    instance: Instance,
    position: Point,
    block: Block,
): Int {
    var height = 0
    var current = position
    while (instance.getBlock(current).key() == block.key()) {
        height++
        current = current.add(0.0, -1.0, 0.0)
    }
    return height
}

private fun BlockRulesetEditor.registerColumn(
    block: Block,
    maxHeight: Int,
    id: String,
) {
    val rule = VerticalGrowthBehaviour(maxHeight).rule("blocksandstuff:${id}_vertical_growth")
    blocks(block) { randomTick.append { install(rule) } }
}
