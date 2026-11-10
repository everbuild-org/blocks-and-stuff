package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import java.util.Random

private const val CAVE_VINE_MAX_AGE = 25
private const val CAVE_VINE_MAX_LENGTH = 25
private const val CAVE_VINE_GROWTH_CHANCE = 10

val CAVE_VINES_GROWTH_RULE: Rule<RandomTickActivityContext> =
    Rule("blocksandstuff:cave_vines_growth") { ctx ->
        if (!InstanceOptionsProvider.getForInstance(ctx.instance).vinesSpread) return@Rule RuleResult.Next
        if (Random().nextInt(CAVE_VINE_GROWTH_CHANCE) != 0) return@Rule RuleResult.Next

        val age = ctx.block.getProperty("age")?.toIntOrNull() ?: 0
        val belowPosition = ctx.blockPosition.add(0.0, -1.0, 0.0)
        val below = ctx.instance.getBlock(belowPosition)

        if (below.air() && ctx.block.compare(Block.CAVE_VINES) && caveVineLength(ctx.instance, ctx.blockPosition) < CAVE_VINE_MAX_LENGTH) {
            ctx.instance.setBlock(
                ctx.blockPosition,
                Block.CAVE_VINES_PLANT.withProperty("berries", ctx.block.getProperty("berries") ?: "false"),
                false,
            )
            ctx.instance.setBlock(
                belowPosition,
                Block.CAVE_VINES
                    .withProperty("age", "0")
                    .withProperty("berries", "false"),
                false,
            )
            return@Rule RuleResult.Next
        }

        if (age < CAVE_VINE_MAX_AGE) {
            ctx.replaceBlock(ctx.block.withProperty("age", (age + 1).toString()))
        } else if (ctx.block.compare(Block.CAVE_VINES) && ctx.block.getProperty("berries") != "true") {
            ctx.replaceBlock(ctx.block.withProperty("berries", "true"))
        }
        RuleResult.Next
    }

val CAVE_VINES_HARVEST_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:cave_vines_harvest") { ctx ->
        if (!ctx.block.compare(Block.CAVE_VINES)) return@Rule RuleResult.Next
        if (ctx.block.getProperty("berries") != "true") return@Rule RuleResult.Next

        val count = 1 + Random().nextInt(2)
        DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ItemStack.of(Material.GLOW_BERRIES, count))
        ctx.replaceBlock(ctx.block.withProperty("berries", "false"))
        playSoundFor(ctx, SoundEvent.BLOCK_CAVE_VINES_PICK_BERRIES)
        RuleResult.Consume
    }

private fun caveVineLength(
    instance: net.minestom.server.instance.Instance,
    position: net.minestom.server.coordinate.Point,
): Int {
    var length = 0
    var current = position
    while (length < CAVE_VINE_MAX_LENGTH + 1) {
        val block = instance.getBlock(current)
        if (!block.compare(Block.CAVE_VINES) && !block.compare(Block.CAVE_VINES_PLANT)) break
        length++
        current = current.add(0.0, 1.0, 0.0)
    }
    return length
}

fun BlockRulesetEditor.addCaveVines() {
    listOf(Block.CAVE_VINES, Block.CAVE_VINES_PLANT).forEach { block ->
        blocks(block) { randomTick.append { install(CAVE_VINES_GROWTH_RULE) } }
    }
    blocks(Block.CAVE_VINES) { interact.append { install(CAVE_VINES_HARVEST_RULE) } }
}
