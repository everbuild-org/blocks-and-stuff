package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.ToolTransform
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.blocks.behavior.tool.toolRule
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val axes =
    listOf(
        Material.WOODEN_AXE,
        Material.STONE_AXE,
        Material.IRON_AXE,
        Material.GOLDEN_AXE,
        Material.DIAMOND_AXE,
        Material.COPPER_AXE,
        Material.NETHERITE_AXE,
    )

private val strippedVariants =
    mapOf(
        Block.OAK_LOG to Block.STRIPPED_OAK_LOG,
        Block.SPRUCE_LOG to Block.STRIPPED_SPRUCE_LOG,
        Block.BIRCH_LOG to Block.STRIPPED_BIRCH_LOG,
        Block.JUNGLE_LOG to Block.STRIPPED_JUNGLE_LOG,
        Block.ACACIA_LOG to Block.STRIPPED_ACACIA_LOG,
        Block.DARK_OAK_LOG to Block.STRIPPED_DARK_OAK_LOG,
        Block.MANGROVE_LOG to Block.STRIPPED_MANGROVE_LOG,
        Block.CHERRY_LOG to Block.STRIPPED_CHERRY_LOG,
        Block.BAMBOO_BLOCK to Block.STRIPPED_BAMBOO_BLOCK,
        Block.OAK_WOOD to Block.STRIPPED_OAK_WOOD,
        Block.SPRUCE_WOOD to Block.STRIPPED_SPRUCE_WOOD,
        Block.BIRCH_WOOD to Block.STRIPPED_BIRCH_WOOD,
        Block.JUNGLE_WOOD to Block.STRIPPED_JUNGLE_WOOD,
        Block.ACACIA_WOOD to Block.STRIPPED_ACACIA_WOOD,
        Block.DARK_OAK_WOOD to Block.STRIPPED_DARK_OAK_WOOD,
        Block.MANGROVE_WOOD to Block.STRIPPED_MANGROVE_WOOD,
        Block.CHERRY_WOOD to Block.STRIPPED_CHERRY_WOOD,
        Block.CRIMSON_STEM to Block.STRIPPED_CRIMSON_STEM,
        Block.WARPED_STEM to Block.STRIPPED_WARPED_STEM,
        Block.CRIMSON_HYPHAE to Block.STRIPPED_CRIMSON_HYPHAE,
        Block.WARPED_HYPHAE to Block.STRIPPED_WARPED_HYPHAE,
    )

private val strippingTransforms: List<ToolTransform> =
    buildList {
        strippedVariants.forEach { (from, to) ->
            axes.forEach { axe ->
                add(
                    ToolTransform(
                        from,
                        axe,
                        to,
                        preserveProperties = true,
                        damagesTool = true,
                        extra = { ctx, _ -> playSoundFor(ctx, SoundEvent.ITEM_AXE_STRIP) },
                    ),
                )
            }
        }
    }

val STRIPPING_RULE: Rule<InteractionActivityContext> = toolRule("blocksandstuff:stripping", strippingTransforms)

fun BlockRulesetEditor.addStripping() {
    strippedVariants.keys.forEach { block ->
        blocks(block) { interact.append { install(STRIPPING_RULE) } }
    }
}
