package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.Random

private const val KELP_MAX_AGE = 25
private const val KELP_GROWTH_CHANCE = 0.14

/**
 * Vanilla-like kelp growth: the head converts to a kelp plant and a new head with the next age is
 * placed into the water above, up to [KELP_MAX_AGE].
 */
class KelpGrowthBehaviour(
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            val age = ctx.block.getProperty("age")?.toIntOrNull() ?: return@Rule RuleResult.Next
            if (age >= KELP_MAX_AGE) return@Rule RuleResult.Next
            if (random.nextDouble() >= KELP_GROWTH_CHANCE) return@Rule RuleResult.Next

            val above = ctx.blockPosition.relative(BlockFace.TOP)
            if (!ctx.instance.getBlock(above).compare(Block.WATER)) return@Rule RuleResult.Next

            ctx.replaceBlock(Block.KELP_PLANT)
            ctx.instance.setBlock(above, Block.KELP.withProperty("age", (age + 1).toString()))
            RuleResult.Next
        }
}

/**
 * Vanilla-like seagrass survival: seagrass needs a full support face below, tall seagrass also
 * needs its counterpart. Because the library has no shape-update pass for these blocks, the check
 * runs on random tick and restores water when the plant is no longer supported.
 */
class SeagrassSurvivalBehaviour {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            val block = ctx.block
            val half = block.getProperty("half")
            val below = ctx.instance.getBlock(ctx.blockPosition.relative(BlockFace.BOTTOM))
            val survives =
                when (half) {
                    "upper" -> below.compare(Block.TALL_SEAGRASS) && below.getProperty("half") == "lower"
                    else -> supportsSeagrass(below)
                }

            if (survives) return@Rule RuleResult.Next
            breakSeagrass(ctx)
            RuleResult.Next
        }

    private fun breakSeagrass(ctx: RandomTickActivityContext) {
        val position = ctx.blockPosition
        val block = ctx.block
        val otherHalf =
            when (block.getProperty("half")) {
                "upper" -> position.relative(BlockFace.BOTTOM)
                "lower" -> position.relative(BlockFace.TOP)
                else -> null
            }

        ctx.instance.setBlock(position, Block.WATER)
        if (otherHalf != null) {
            val sibling = ctx.instance.getBlock(otherHalf)
            if (sibling.compare(Block.TALL_SEAGRASS)) {
                ctx.instance.setBlock(otherHalf, Block.WATER)
            }
        }
    }
}

private fun supportsSeagrass(below: Block): Boolean =
    below.collisionShape().isFaceFull(BlockFace.TOP) && !below.compare(Block.MAGMA_BLOCK)

fun BlockRulesetEditor.addKelp() {
    val rule = KelpGrowthBehaviour().rule("blocksandstuff:kelp_growth")
    blocks(Block.KELP) { randomTick.append { install(rule) } }
}

fun BlockRulesetEditor.addSeagrass() {
    val rule = SeagrassSurvivalBehaviour().rule("blocksandstuff:seagrass_survival")
    blocks(Block.SEAGRASS) { randomTick.append { install(rule) } }
    blocks(Block.TALL_SEAGRASS) { randomTick.append { install(rule) } }
}
