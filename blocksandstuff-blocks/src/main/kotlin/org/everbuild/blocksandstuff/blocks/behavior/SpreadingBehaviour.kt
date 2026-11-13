package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.Random

private const val SPREAD_CHANCE = 8
private const val DECAY_CHANCE = 4
private const val MIN_SPREAD_LIGHT = 4

private data class SpreadingRule(
    val source: Block,
    val target: Block,
    val minLight: Int = MIN_SPREAD_LIGHT,
    val spreads: Boolean = true,
)

private val spreadingRules =
    listOf(
        SpreadingRule(Block.GRASS_BLOCK, Block.DIRT),
        SpreadingRule(Block.MYCELIUM, Block.DIRT),
        SpreadingRule(Block.PODZOL, Block.DIRT, spreads = false),
        SpreadingRule(Block.CRIMSON_NYLIUM, Block.NETHERRACK, minLight = 0),
        SpreadingRule(Block.WARPED_NYLIUM, Block.NETHERRACK, minLight = 0),
    )

val SPREADING_RULE: Rule<RandomTickActivityContext> =
    Rule("blocksandstuff:spreading") { ctx ->
        val spreading =
            spreadingRules.firstOrNull { it.source.compare(ctx.block) } ?: return@Rule RuleResult.Next
        val random = Random()

        if (shouldDecay(ctx, spreading) && random.nextInt(DECAY_CHANCE) == 0) {
            ctx.replaceBlock(spreading.target)
            return@Rule RuleResult.Next
        }

        if (random.nextInt(SPREAD_CHANCE) != 0) return@Rule RuleResult.Next
        if (!spreading.spreads) return@Rule RuleResult.Next

        val spread = findSpreadTarget(ctx, spreading) ?: return@Rule RuleResult.Next
        ctx.instance.setBlock(spread, spreading.source)
        RuleResult.Next
    }

private fun shouldDecay(
    ctx: RandomTickActivityContext,
    spreading: SpreadingRule,
): Boolean {
    val abovePosition = ctx.blockPosition.add(0.0, 1.0, 0.0)
    val above = ctx.instance.getBlock(abovePosition)
    if (above.air()) return false
    if (!isLightBlocking(above)) return false
    return !hasEnoughLight(ctx.instance, abovePosition, MIN_SPREAD_LIGHT)
}

private fun isLightBlocking(block: Block): Boolean =
    block.collisionShape().isFaceFull(BlockFace.TOP) && block.collisionShape().isFaceFull(BlockFace.BOTTOM)

private fun findSpreadTarget(
    ctx: RandomTickActivityContext,
    spreading: SpreadingRule,
): net.minestom.server.coordinate.Point? {
    val offsets =
        listOf(
            Triple(1, 0, 0),
            Triple(-1, 0, 0),
            Triple(0, 0, 1),
            Triple(0, 0, -1),
            Triple(1, 1, 0),
            Triple(-1, 1, 0),
            Triple(0, 1, 1),
            Triple(0, 1, -1),
            Triple(1, -1, 0),
            Triple(-1, -1, 0),
            Triple(0, -1, 1),
            Triple(0, -1, -1),
        ).shuffled()

    for ((dx, dy, dz) in offsets) {
        val target = ctx.blockPosition.add(dx.toDouble(), dy.toDouble(), dz.toDouble())
        if (!ctx.instance.getBlock(target).compare(spreading.target)) continue
        val aboveTarget = target.add(0.0, 1.0, 0.0)
        if (!ctx.instance.getBlock(aboveTarget).air()) continue
        if (!hasEnoughLight(ctx.instance, aboveTarget, spreading.minLight)) continue
        return target
    }
    return null
}

private fun hasEnoughLight(
    instance: net.minestom.server.instance.Instance,
    position: net.minestom.server.coordinate.Point,
    minLight: Int,
): Boolean {
    if (minLight <= 0) return true
    return maxOf(
        instance.getSkyLight(position.blockX(), position.blockY(), position.blockZ()),
        instance.getBlockLight(position.blockX(), position.blockY(), position.blockZ()),
    ) >= minLight
}

fun BlockRulesetEditor.addSpreading() {
    spreadingRules.map { it.source }.distinctBy { it.key() }.forEach { block ->
        blocks(block) { randomTick.append { install(SPREADING_RULE) } }
    }
}
