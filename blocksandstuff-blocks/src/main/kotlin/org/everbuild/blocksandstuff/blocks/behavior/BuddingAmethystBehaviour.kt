package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.Random

private const val AMETHYST_GROWTH_CHANCE = 5

/**
 * Vanilla-like budding amethyst growth: on each random tick a random face grows or advances one
 * amethyst bud/cluster stage.
 */
class BuddingAmethystBehaviour(
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            if (random.nextInt(AMETHYST_GROWTH_CHANCE) != 0) return@Rule RuleResult.Next

            val face = BlockFace.entries[random.nextInt(BlockFace.entries.size)]
            val targetPosition = ctx.blockPosition.relative(face)
            val target = ctx.instance.getBlock(targetPosition)
            val next =
                when {
                    canGrowInto(target) -> Block.SMALL_AMETHYST_BUD
                    target.compare(Block.SMALL_AMETHYST_BUD) -> Block.MEDIUM_AMETHYST_BUD
                    target.compare(Block.MEDIUM_AMETHYST_BUD) -> Block.LARGE_AMETHYST_BUD
                    target.compare(Block.LARGE_AMETHYST_BUD) -> Block.AMETHYST_CLUSTER
                    else -> return@Rule RuleResult.Next
                }

            val waterlogged = isWaterSource(target)
            ctx.instance.setBlock(
                targetPosition,
                next
                    .withProperty("facing", facingValue(face))
                    .withProperty("waterlogged", waterlogged.toString()),
            )
            RuleResult.Next
        }
}

private fun facingValue(face: BlockFace): String =
    when (face) {
        BlockFace.TOP -> "up"
        BlockFace.BOTTOM -> "down"
        else -> face.name.lowercase()
    }

private fun canGrowInto(block: Block): Boolean = block.air() || isWaterSource(block)

private fun isWaterSource(block: Block): Boolean =
    block.compare(Block.WATER) && (block.getProperty("level") ?: "0") == "0"

fun BlockRulesetEditor.addBuddingAmethyst() {
    val rule = BuddingAmethystBehaviour().rule("blocksandstuff:budding_amethyst")
    blocks(Block.BUDDING_AMETHYST) { randomTick.append { install(rule) } }
}
