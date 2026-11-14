package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.ToolTransform
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.blocks.behavior.tool.toolRule
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val shovels =
    listOf(
        Material.WOODEN_SHOVEL,
        Material.STONE_SHOVEL,
        Material.IRON_SHOVEL,
        Material.GOLDEN_SHOVEL,
        Material.DIAMOND_SHOVEL,
        Material.COPPER_SHOVEL,
        Material.NETHERITE_SHOVEL,
    )

private val flattenables =
    listOf(
        Block.GRASS_BLOCK,
        Block.DIRT,
        Block.COARSE_DIRT,
        Block.PODZOL,
        Block.MYCELIUM,
        Block.ROOTED_DIRT,
    )

private fun hasAirAbove(ctx: InteractionActivityContext): Boolean =
    ctx.instance.getBlock(ctx.blockPosition.add(0.0, 1.0, 0.0)).air()

private val shovelTransforms: List<ToolTransform> =
    buildList {
        flattenables.forEach { from ->
            shovels.forEach { shovel ->
                add(
                    ToolTransform(
                        from,
                        shovel,
                        Block.DIRT_PATH,
                        damagesTool = true,
                        guard = ::hasAirAbove,
                        extra = { ctx, _ -> playSoundFor(ctx, SoundEvent.ITEM_SHOVEL_FLATTEN) },
                    ),
                )
            }
        }
    }

val SHOVEL_RULE: Rule<InteractionActivityContext> = toolRule("blocksandstuff:shovel", shovelTransforms)

val CAMPFIRE_EXTINGUISH_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:campfire_extinguish") { ctx ->
        if (!shovels.contains(ctx.player.itemInMainHand.material())) return@Rule RuleResult.Next
        if (ctx.block.getProperty("lit") != "true") return@Rule RuleResult.Next

        ctx.replaceBlock(ctx.block.withProperty("lit", "false"))
        playSoundFor(ctx, SoundEvent.BLOCK_FIRE_EXTINGUISH)
        RuleResult.Consume
    }

fun BlockRulesetEditor.addShovels() {
    flattenables.forEach { block ->
        blocks(block) { interact.append { install(SHOVEL_RULE) } }
    }
    listOf(Block.CAMPFIRE, Block.SOUL_CAMPFIRE).forEach { block ->
        blocks(block) { interact.append { install(CAMPFIRE_EXTINGUISH_RULE) } }
    }
}
