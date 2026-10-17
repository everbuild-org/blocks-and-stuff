package org.everbuild.blocksandstuff.blocks.behavior

import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick

/**
 * Shared random-tick growth for `age`-based plants: advances `age` by one up to the block's own
 * maximum age when the light level suffices and the random growth check passes.
 */
class PlantGrowthBehaviour(
    private val growthChance: Int,
    private val minLight: Int = 9,
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            val age = ctx.block.getProperty("age")?.toIntOrNull() ?: return@Rule RuleResult.Next
            if (age >= maxAgeOf(ctx.block)) return@Rule RuleResult.Next
            if (!GrowthLight.hasEnoughLight(ctx.instance, ctx.blockPosition, minLight)) return@Rule RuleResult.Next
            if (random.nextInt(growthChance) != 0) return@Rule RuleResult.Next

            ctx.replaceBlock(ctx.block.withProperty("age", (age + 1).toString()))
            RuleResult.Next
        }
}

private const val CROPS_TAG = "minecraft:crops"

internal val CROP_BLOCKS: Collection<Block> = BlockTags.getTaggedWith(CROPS_TAG)

private val maxAgeCache = ConcurrentHashMap<Key, Int>()

internal fun maxAgeOf(block: Block): Int =
    maxAgeCache.computeIfAbsent(block.key()) {
        block
            .possibleStates()
            .mapNotNull { it.getProperty("age")?.toIntOrNull() }
            .maxOrNull() ?: 0
    }

fun BlockRulesetEditor.addPlantGrowth() {
    val covered = HashSet<Key>()
    addAgeGrowth(CROP_BLOCKS, "crop", growthChance = 8, minLight = 9, covered)
    addAgeGrowth(listOf(Block.NETHER_WART), "nether_wart", growthChance = 10, minLight = 0, covered)
    addAgeGrowth(listOf(Block.TORCHFLOWER_CROP, Block.PITCHER_CROP), "flower_crop", growthChance = 8, minLight = 9, covered)
}

private fun BlockRulesetEditor.addAgeGrowth(
    family: Collection<Block>,
    id: String,
    growthChance: Int,
    minLight: Int,
    covered: MutableSet<Key>,
) {
    val targets = family.filter { covered.add(it.key()) }
    if (targets.isEmpty()) return

    val rule = PlantGrowthBehaviour(growthChance, minLight).rule("blocksandstuff:${id}_growth")
    targets.forEach { block ->
        blocks(block) { randomTick.append { install(rule) } }
    }
}

