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
 * Shared random-tick growth for fruit stems (pumpkin/melon): advances `age` and, once fully grown,
 * places the fruit on a neighbouring support block.
 */
class StemGrowthBehaviour(
    private val fruit: Block,
    private val attachedStem: Block,
    private val growthChance: Int = 8,
    private val minLight: Int = 9,
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            val age = ctx.block.getProperty("age")?.toIntOrNull() ?: return@Rule RuleResult.Next
            if (!GrowthLight.hasEnoughLight(ctx.instance, ctx.blockPosition, minLight)) return@Rule RuleResult.Next
            if (random.nextInt(growthChance) != 0) return@Rule RuleResult.Next

            if (age < MAX_AGE) {
                ctx.replaceBlock(ctx.block.withProperty("age", (age + 1).toString()))
            } else {
                growFruit(ctx.instance, ctx.blockPosition)
            }
            RuleResult.Next
        }

    private fun growFruit(
        instance: Instance,
        position: Point,
    ) {
        for ((dx, dz, facing) in DIRECTIONS.shuffled(random)) {
            val target = position.add(dx.toDouble(), 0.0, dz.toDouble())
            if (!instance.getBlock(target).air()) continue
            if (!isSupport(instance.getBlock(target.add(0.0, -1.0, 0.0)))) continue
            instance.setBlock(target, fruit)
            instance.setBlock(position, attachedStem.withProperty("facing", facing), false)
            return
        }
    }

    private fun isSupport(block: Block): Boolean = SUPPORTS.any { block.compare(it) }

    private companion object {
        private const val MAX_AGE = 7
        private val DIRECTIONS = listOf(Triple(0, 1, "south"), Triple(0, -1, "north"), Triple(1, 0, "east"), Triple(-1, 0, "west"))
        private val SUPPORTS =
            listOf(
                Block.FARMLAND,
                Block.DIRT,
                Block.COARSE_DIRT,
                Block.ROOTED_DIRT,
                Block.GRASS_BLOCK,
                Block.PODZOL,
                Block.MUD,
                Block.MOSS_BLOCK,
            )
    }
}

fun BlockRulesetEditor.addStemGrowth() {
    registerStem(Block.PUMPKIN_STEM, Block.PUMPKIN, Block.ATTACHED_PUMPKIN_STEM, "pumpkin")
    registerStem(Block.MELON_STEM, Block.MELON, Block.ATTACHED_MELON_STEM, "melon")
}

private fun BlockRulesetEditor.registerStem(
    stem: Block,
    fruit: Block,
    attachedStem: Block,
    id: String,
) {
    val rule = StemGrowthBehaviour(fruit, attachedStem).rule("blocksandstuff:${id}_stem_growth")
    blocks(stem) { randomTick.append { install(rule) } }
}

