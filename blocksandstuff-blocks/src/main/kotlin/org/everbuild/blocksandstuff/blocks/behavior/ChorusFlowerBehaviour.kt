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
import java.util.Random

private const val CHORUS_MAX_AGE = 5
private const val PILLAR_SCAN_DEPTH = 4
private const val MIN_PILLAR_HEIGHT = 2
private const val GROWTH_RANDOM_RANGE = 4
private const val GROWTH_RANDOM_RANGE_ON_SUPPORT = 5
private const val BRANCH_ATTEMPT_RANDOM_RANGE = 4
private val HORIZONTAL_FACES = listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)

/**
 * Vanilla-like chorus flower growth: grows upward along a pillar, creates horizontal branches
 * when upward growth is blocked and turns into a dead (age 5) flower when no branch is possible.
 */
class ChorusFlowerBehaviour(
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            val above = ctx.blockPosition.relative(BlockFace.TOP)
            if (!ctx.instance.getBlock(above).air()) return@Rule RuleResult.Next

            val age = ctx.block.getProperty("age")?.toIntOrNull() ?: return@Rule RuleResult.Next
            if (age >= CHORUS_MAX_AGE) return@Rule RuleResult.Next

            val below = ctx.instance.getBlock(ctx.blockPosition.relative(BlockFace.BOTTOM))
            var growUpwards = false
            var pillarOnSupportBlock = false

            if (supportsChorus(below)) {
                growUpwards = true
            } else if (below.compare(Block.CHORUS_PLANT)) {
                var height = 1
                var scanned = 0
                while (scanned < PILLAR_SCAN_DEPTH) {
                    val test = ctx.instance.getBlock(ctx.blockPosition.add(0.0, -(height + 1).toDouble(), 0.0))
                    if (!test.compare(Block.CHORUS_PLANT)) {
                        pillarOnSupportBlock = supportsChorus(test)
                        break
                    }
                    height++
                    scanned++
                }

                val range = if (pillarOnSupportBlock) GROWTH_RANDOM_RANGE_ON_SUPPORT else GROWTH_RANDOM_RANGE
                if (height < MIN_PILLAR_HEIGHT || height <= random.nextInt(range)) {
                    growUpwards = true
                }
            } else if (below.air()) {
                growUpwards = true
            }

            if (growUpwards && allNeighborsEmpty(ctx.instance, above, null) && ctx.instance.getBlock(above.relative(BlockFace.TOP)).air()) {
                ctx.instance.setBlock(above, Block.CHORUS_FLOWER.withProperty("age", age.toString()))
                ctx.replaceBlock(chorusPlantWithConnections(ctx.instance, ctx.blockPosition))
            } else if (age < CHORUS_MAX_AGE - 1) {
                growBranches(ctx, age, pillarOnSupportBlock)
            } else {
                ctx.replaceBlock(Block.CHORUS_FLOWER.withProperty("age", CHORUS_MAX_AGE.toString()))
            }
            RuleResult.Next
        }

    private fun growBranches(
        ctx: RandomTickActivityContext,
        age: Int,
        pillarOnSupportBlock: Boolean,
    ) {
        var branchAttempts = random.nextInt(BRANCH_ATTEMPT_RANDOM_RANGE)
        if (pillarOnSupportBlock) branchAttempts += 1

        var createdBranch = false
        repeat(branchAttempts) {
            val direction = HORIZONTAL_FACES[random.nextInt(HORIZONTAL_FACES.size)]
            val target = ctx.blockPosition.relative(direction)
            val targetBelow = target.relative(BlockFace.BOTTOM)
            if (!ctx.instance.getBlock(target).air()) return@repeat
            if (!ctx.instance.getBlock(targetBelow).air()) return@repeat
            if (!allNeighborsEmpty(ctx.instance, target, direction.oppositeFace)) return@repeat

            ctx.instance.setBlock(target, Block.CHORUS_FLOWER.withProperty("age", (age + 1).toString()))
            createdBranch = true
        }

        if (createdBranch) {
            ctx.replaceBlock(chorusPlantWithConnections(ctx.instance, ctx.blockPosition))
        } else {
            ctx.replaceBlock(Block.CHORUS_FLOWER.withProperty("age", CHORUS_MAX_AGE.toString()))
        }
    }
}

private fun allNeighborsEmpty(
    instance: Instance,
    position: Point,
    ignore: BlockFace?,
): Boolean =
    HORIZONTAL_FACES.all { face ->
        face == ignore || instance.getBlock(position.relative(face)).air()
    }

private fun supportsChorus(block: Block): Boolean =
    block.compare(Block.END_STONE) || BlockTags.hasTag(block, "minecraft:supports_chorus_flower")

private fun chorusPlantWithConnections(
    instance: Instance,
    position: Point,
): Block {
    fun connects(
        face: BlockFace,
        supportCounts: Boolean,
    ): Boolean {
        val neighbor = instance.getBlock(position.relative(face))
        if (neighbor.compare(Block.CHORUS_PLANT) || neighbor.compare(Block.CHORUS_FLOWER)) return true
        return supportCounts && supportsChorus(neighbor)
    }

    return Block.CHORUS_PLANT
        .withProperty("down", connects(BlockFace.BOTTOM, supportCounts = true).toString())
        .withProperty("up", connects(BlockFace.TOP, supportCounts = false).toString())
        .withProperty("north", connects(BlockFace.NORTH, supportCounts = false).toString())
        .withProperty("east", connects(BlockFace.EAST, supportCounts = false).toString())
        .withProperty("south", connects(BlockFace.SOUTH, supportCounts = false).toString())
        .withProperty("west", connects(BlockFace.WEST, supportCounts = false).toString())
}

fun BlockRulesetEditor.addChorusFlowers() {
    val rule = ChorusFlowerBehaviour().rule("blocksandstuff:chorus_flower")
    blocks(Block.CHORUS_FLOWER) { randomTick.append { install(rule) } }
}
