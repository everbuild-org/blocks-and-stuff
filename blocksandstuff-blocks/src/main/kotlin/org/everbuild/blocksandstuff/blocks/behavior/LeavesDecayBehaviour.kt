package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.ArrayDeque
import java.util.HashSet

private const val LEAVES_DECAY_DISTANCE = 7

private val LEAF_KEYS: Set<String> =
    BlockTags.getTaggedWith("minecraft:leaves").mapTo(HashSet()) { it.key().asString() }
private val LOG_KEYS: Set<String> =
    BlockTags.getTaggedWith("minecraft:logs").mapTo(HashSet()) { it.key().asString() }

/**
 * Vanilla-like leaf decay: a non-persistent leaf without a log within six steps through connected
 * leaves (vanilla distance 7) is removed.
 */
class LeavesDecayBehaviour {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            if (ctx.block.getProperty("persistent") == "true") return@Rule RuleResult.Next
            if (hasNearbyLog(ctx.instance, ctx.blockPosition)) return@Rule RuleResult.Next

            val waterlogged = ctx.block.getProperty("waterlogged") == "true"
            ctx.instance.setBlock(ctx.blockPosition, if (waterlogged) Block.WATER else Block.AIR)
            RuleResult.Next
        }
}

private fun hasNearbyLog(
    instance: Instance,
    origin: Point,
): Boolean {
    val visited = HashSet<Point>()
    val queue = ArrayDeque<Pair<Point, Int>>()
    visited.add(origin)
    queue.add(origin to 0)

    while (queue.isNotEmpty()) {
        val (position, distance) = queue.removeFirst()
        if (distance >= LEAVES_DECAY_DISTANCE - 1) continue

        for (face in BlockFace.entries) {
            val neighbor = position.relative(face)
            if (!visited.add(neighbor)) continue

            val block = instance.getBlock(neighbor)
            val key = block.key().asString()
            if (key in LOG_KEYS) return true
            if (key in LEAF_KEYS) queue.add(neighbor to distance + 1)
        }
    }
    return false
}

private fun leafBlocks(): List<Block> =
    BlockTags
        .getTaggedWith("minecraft:leaves")
        .ifEmpty { Block.values().filter { it.key().asString().endsWith("_leaves") }.toSet() }
        .toList()

fun BlockRulesetEditor.addLeavesDecay() {
    val rule = LeavesDecayBehaviour().rule("blocksandstuff:leaves_decay")
    leafBlocks().forEach { block ->
        blocks(block) { randomTick.append { install(rule) } }
    }
}
