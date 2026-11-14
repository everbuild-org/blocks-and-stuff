package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.util.LEAVES_DECAY_DISTANCE
import org.everbuild.blocksandstuff.blocks.util.leavesDistance
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.Random

private const val SAPLING_CHANCE = 0.05
private const val JUNGLE_SAPLING_CHANCE = 0.025
private const val APPLE_CHANCE = 0.005
private val APPLE_LEAVES = setOf("oak_leaves", "dark_oak_leaves")

/**
 * Vanilla-like leaf decay: a non-persistent leaf at distance 7 is removed and drops its vanilla
 * sapling/apple loot. Stale distance values are repaired with a bounded leaf-distance lookup.
 */
class LeavesDecayBehaviour(
    private val random: Random = Random(),
) {
    fun rule(id: String): Rule<RandomTickActivityContext> =
        Rule(id) { ctx ->
            if (ctx.block.getProperty("persistent") == "true") return@Rule RuleResult.Next

            val distance = ctx.block.getProperty("distance")?.toIntOrNull() ?: LEAVES_DECAY_DISTANCE
            if (distance < LEAVES_DECAY_DISTANCE) return@Rule RuleResult.Next

            val actualDistance = leavesDistance(ctx.instance, ctx.blockPosition)
            if (actualDistance < LEAVES_DECAY_DISTANCE) {
                ctx.replaceBlock(ctx.block.withProperty("distance", actualDistance.toString()))
                return@Rule RuleResult.Next
            }

            dropLeaves(ctx.instance, ctx.block, ctx.blockPosition)
            val waterlogged = ctx.block.getProperty("waterlogged") == "true"
            ctx.instance.setBlock(ctx.blockPosition, if (waterlogged) Block.WATER else Block.AIR)
            RuleResult.Next
        }

    private fun dropLeaves(
        instance: Instance,
        block: Block,
        position: Point,
    ) {
        val leafKey = block.key().value()

        if (leafKey in APPLE_LEAVES && random.nextDouble() < APPLE_CHANCE) {
            DroppedItemFactory.maybeDrop(instance, position, ItemStack.of(Material.APPLE))
        }

        val sapling = saplingFor(leafKey) ?: return
        val chance = if (leafKey == "jungle_leaves") JUNGLE_SAPLING_CHANCE else SAPLING_CHANCE
        if (random.nextDouble() < chance) {
            DroppedItemFactory.maybeDrop(instance, position, ItemStack.of(sapling))
        }
    }
}

private fun saplingFor(leafKey: String): Material? {
    val saplingKey =
        when (leafKey) {
            "azalea_leaves" -> "azalea"
            "flowering_azalea_leaves" -> "flowering_azalea"
            "mangrove_leaves" -> "mangrove_propagule"
            "red_poplar_leaves", "orange_poplar_leaves", "yellow_poplar_leaves" -> "poplar_sapling"
            else -> leafKey.removeSuffix("_leaves") + "_sapling"
        }
    return Material.fromKey(Key.key("minecraft", saplingKey))
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

