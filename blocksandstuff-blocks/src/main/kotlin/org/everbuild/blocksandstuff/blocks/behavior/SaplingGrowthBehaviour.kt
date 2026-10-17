package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.BlockVec
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
 * Shared random-tick growth for saplings. A 2x2 arrangement of the same sapling grows a large tree
 * (jungle, spruce, dark oak) where supported; otherwise the single-sapling tree is used. Custom
 * generators attached to the instance via [TREE_GENERATORS] take precedence.
 */
class SaplingGrowthBehaviour(
    private val saplingKey: Key,
    private val growthChance: Int = 7,
    private val minLight: Int = 9,
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            if (!GrowthLight.hasEnoughLight(ctx.instance, ctx.blockPosition, minLight)) return@Rule RuleResult.Next
            if (!ctx.instance.getBlock(ctx.blockPosition.add(0.0, 1.0, 0.0)).air()) return@Rule RuleResult.Next
            if (random.nextInt(growthChance) != 0) return@Rule RuleResult.Next

            growSapling(ctx.instance, ctx.blockPosition, ctx.block, random)
            RuleResult.Next
        }
}

internal fun growSapling(
    instance: Instance,
    position: Point,
    block: Block,
    random: Random,
): Boolean {
    val override = instance.getTag(TREE_GENERATORS)?.get(block.key())
    if (override != null) {
        instance.setBlock(position, Block.AIR)
        override.generate(instance, position, random)
        return true
    }

    val anchor = findSaplingAnchor(instance, position, block)
    if (anchor != null) {
        val large = defaultLargeTreeGenerator(block.key())
        if (large != null) {
            for (dx in 0..1) {
                for (dz in 0..1) {
                    instance.setBlock(BlockVec(anchor.blockX() + dx, anchor.blockY(), anchor.blockZ() + dz), Block.AIR)
                }
            }
            large.generate(instance, anchor, random)
            return true
        }
    }

    val single = defaultTreeGenerator(block.key()) ?: return false
    instance.setBlock(position, Block.AIR)
    single.generate(instance, position, random)
    return true
}

private fun findSaplingAnchor(
    instance: Instance,
    position: Point,
    block: Block,
): BlockVec? {
    val x = position.blockX()
    val y = position.blockY()
    val z = position.blockZ()
    val offsets = listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1)
    for (ax in intArrayOf(x, x - 1)) {
        for (az in intArrayOf(z, z - 1)) {
            val complete = offsets.all { (dx, dz) -> instance.getBlock(BlockVec(ax + dx, y, az + dz)).key() == block.key() }
            if (complete) return BlockVec(ax, y, az)
        }
    }
    return null
}

internal val SAPLING_BLOCKS: List<Block> =
    listOf(
        Block.OAK_SAPLING,
        Block.BIRCH_SAPLING,
        Block.SPRUCE_SAPLING,
        Block.JUNGLE_SAPLING,
        Block.ACACIA_SAPLING,
        Block.DARK_OAK_SAPLING,
        Block.CHERRY_SAPLING,
        Block.PALE_OAK_SAPLING,
        Block.POPLAR_SAPLING,
    )

fun BlockRulesetEditor.addSaplingGrowth() {
    SAPLING_BLOCKS.forEach { sapling ->
        if (defaultTreeGenerator(sapling.key()) == null && defaultLargeTreeGenerator(sapling.key()) == null) return@forEach
        val rule =
            SaplingGrowthBehaviour(sapling.key())
                .rule("blocksandstuff:${sapling.key().value()}_growth")
        blocks(sapling) { randomTick.append { install(rule) } }
    }
}
