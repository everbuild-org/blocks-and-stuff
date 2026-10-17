package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.ToolTransform
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.blocks.behavior.tool.toolRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val hoes =
    listOf(
        Material.WOODEN_HOE,
        Material.STONE_HOE,
        Material.IRON_HOE,
        Material.GOLDEN_HOE,
        Material.DIAMOND_HOE,
        Material.COPPER_HOE,
        Material.NETHERITE_HOE,
    )

private val tillables =
    mapOf(
        Block.GRASS_BLOCK to Block.FARMLAND,
        Block.DIRT to Block.FARMLAND,
        Block.COARSE_DIRT to Block.FARMLAND,
        Block.DIRT_PATH to Block.FARMLAND,
    )

private fun hasAirAbove(ctx: InteractionActivityContext): Boolean = ctx.instance.getBlock(ctx.blockPosition.add(0.0, 1.0, 0.0)).air()

private val hoeingTransforms: List<ToolTransform> =
    buildList {
        tillables.forEach { (from, tillable) ->
            hoes.forEach { hoe ->
                add(
                    ToolTransform(
                        from,
                        hoe,
                        tillable,
                        damagesTool = true,
                        guard = ::hasAirAbove,
                        extra = { ctx, _ -> playSoundFor(ctx, SoundEvent.ITEM_HOE_TILL) },
                    ),
                )
            }
        }
        hoes.forEach { hoe ->
            add(
                ToolTransform(
                    Block.ROOTED_DIRT,
                    hoe,
                    Block.DIRT,
                    damagesTool = true,
                    extra = { ctx, _ ->
                        playSoundFor(ctx, SoundEvent.ITEM_HOE_TILL)
                        DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ItemStack.of(Material.HANGING_ROOTS))
                    },
                ),
            )
        }
    }

val HOEING_RULE: Rule<InteractionActivityContext> = toolRule("blocksandstuff:hoeing", hoeingTransforms)

fun BlockRulesetEditor.addHoes() {
    (tillables.keys + Block.ROOTED_DIRT).forEach { block ->
        blocks(block) { interact.append { install(HOEING_RULE) } }
    }
}
