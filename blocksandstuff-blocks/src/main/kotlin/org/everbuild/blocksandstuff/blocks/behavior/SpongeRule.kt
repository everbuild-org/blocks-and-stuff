package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.PlacementActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.place
import org.everbuild.twaddle.stacked_behaviour_block.type.tick

private const val MAX_ABSORBED_WATER_BLOCKS = 64
private const val MAX_WATER_SEARCH_DEPTH = 6

val SPONGE_PLACE_RULE: Rule<PlacementActivityContext> =
    Rule("blocksandstuff:sponge_place") { ctx ->
        ctx.instance.scheduler().scheduleNextTick {
            absorbWater(ctx.instance, ctx.blockPosition.asBlockVec())
        }
        RuleResult.Next
    }

val SPONGE_TICK_RULE: Rule<TickActivityContext> =
    Rule("blocksandstuff:sponge_tick") { ctx ->
        if (!ctx.block.compare(Block.SPONGE)) return@Rule RuleResult.Next
        val position = ctx.blockPosition.asBlockVec()
        if (!touchesWater(ctx.instance, position)) return@Rule RuleResult.Next

        absorbWater(ctx.instance, position)
        RuleResult.Next
    }

internal fun absorbWater(
    instance: Instance,
    spongePosition: BlockVec,
): Boolean {
    if (!instance.getBlock(spongePosition).compare(Block.SPONGE)) return false

    val queue = ArrayDeque<Pair<BlockVec, Int>>()
    val visited = HashSet<BlockVec>()
    queue.addLast(spongePosition to 0)
    visited.add(spongePosition)

    var absorbed = 0
    while (queue.isNotEmpty() && absorbed < MAX_ABSORBED_WATER_BLOCKS) {
        val (position, depth) = queue.removeFirst()
        for (face in BlockFace.entries) {
            val neighbor = position.relative(face)
            if (!visited.add(neighbor)) continue
            if (!instance.getBlock(neighbor).isWater()) continue

            instance.setBlock(neighbor, Block.AIR, false)
            absorbed++
            if (depth < MAX_WATER_SEARCH_DEPTH) {
                queue.addLast(neighbor to depth + 1)
            }
            if (absorbed >= MAX_ABSORBED_WATER_BLOCKS) break
        }
    }

    if (absorbed == 0) return false
    instance.setBlock(spongePosition, Block.WET_SPONGE)
    return true
}

private fun touchesWater(
    instance: Instance,
    position: BlockVec,
): Boolean = BlockFace.entries.any { instance.getBlock(position.relative(it)).isWater() }

fun BlockRulesetEditor.addSponges() {
    blocks(Block.SPONGE) {
        place.append { install(SPONGE_PLACE_RULE) }
        tick.append { install(SPONGE_TICK_RULE) }
    }
}
