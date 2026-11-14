package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick

val CORAL_RULE: Rule<RandomTickActivityContext> =
    Rule("blocksandstuff:coral") { ctx ->
        val current = ctx.block
        val isCoralBlock = current.defaultState() in CORAL_BLOCKS

        if (!isCoralBlock) {
            if (current.getProperty("waterlogged") == "true") return@Rule RuleResult.Next
        } else {
            var hasAdjacentWater = false
            for (face in BlockFace.entries) {
                val neighbor = ctx.instance.getBlock(ctx.blockPosition.add(face.toDirection().vec()))
                if (neighbor.isWater() || neighbor.getProperty("waterlogged") == "true") {
                    hasAdjacentWater = true
                    break
                }
            }
            if (hasAdjacentWater) return@Rule RuleResult.Next
        }

        val dead = DEAD_VARIANTS[current.defaultState()] ?: return@Rule RuleResult.Next
        var result = dead.withDefaultHandler()
        current.getProperty("facing")?.let { facing ->
            result = result.withProperty("facing", facing)
        }
        if (result.getProperty("waterlogged") != null) {
            result = result.withProperty("waterlogged", "false")
        }
        ctx.replaceBlock(result)
        RuleResult.Next
    }

private val CORAL_BLOCKS =
    setOf(
        Block.TUBE_CORAL_BLOCK,
        Block.BRAIN_CORAL_BLOCK,
        Block.BUBBLE_CORAL_BLOCK,
        Block.FIRE_CORAL_BLOCK,
        Block.HORN_CORAL_BLOCK,
    )

private val DEAD_VARIANTS =
    mapOf(
        Block.TUBE_CORAL to Block.DEAD_TUBE_CORAL,
        Block.BRAIN_CORAL to Block.DEAD_BRAIN_CORAL,
        Block.BUBBLE_CORAL to Block.DEAD_BUBBLE_CORAL,
        Block.FIRE_CORAL to Block.DEAD_FIRE_CORAL,
        Block.HORN_CORAL to Block.DEAD_HORN_CORAL,
        Block.TUBE_CORAL_FAN to Block.DEAD_TUBE_CORAL_FAN,
        Block.BRAIN_CORAL_FAN to Block.DEAD_BRAIN_CORAL_FAN,
        Block.BUBBLE_CORAL_FAN to Block.DEAD_BUBBLE_CORAL_FAN,
        Block.FIRE_CORAL_FAN to Block.DEAD_FIRE_CORAL_FAN,
        Block.HORN_CORAL_FAN to Block.DEAD_HORN_CORAL_FAN,
        Block.TUBE_CORAL_WALL_FAN to Block.DEAD_TUBE_CORAL_WALL_FAN,
        Block.BRAIN_CORAL_WALL_FAN to Block.DEAD_BRAIN_CORAL_WALL_FAN,
        Block.BUBBLE_CORAL_WALL_FAN to Block.DEAD_BUBBLE_CORAL_WALL_FAN,
        Block.FIRE_CORAL_WALL_FAN to Block.DEAD_FIRE_CORAL_WALL_FAN,
        Block.HORN_CORAL_WALL_FAN to Block.DEAD_HORN_CORAL_WALL_FAN,
        Block.TUBE_CORAL_BLOCK to Block.DEAD_TUBE_CORAL_BLOCK,
        Block.BRAIN_CORAL_BLOCK to Block.DEAD_BRAIN_CORAL_BLOCK,
        Block.BUBBLE_CORAL_BLOCK to Block.DEAD_BUBBLE_CORAL_BLOCK,
        Block.FIRE_CORAL_BLOCK to Block.DEAD_FIRE_CORAL_BLOCK,
        Block.HORN_CORAL_BLOCK to Block.DEAD_HORN_CORAL_BLOCK,
    )

private fun coralBlocks(): List<Block> =
    (
        BlockTags.getTaggedWith("minecraft:corals") +
            BlockTags.getTaggedWith("minecraft:coral_plants") +
            BlockTags.getTaggedWith("minecraft:coral_blocks") +
            BlockTags.getTaggedWith("minecraft:wall_corals")
    ).distinct()

fun BlockRulesetEditor.addCorals() {
    coralBlocks().forEach { block ->
        blocks(block) { randomTick.append { install(CORAL_RULE) } }
    }
}
