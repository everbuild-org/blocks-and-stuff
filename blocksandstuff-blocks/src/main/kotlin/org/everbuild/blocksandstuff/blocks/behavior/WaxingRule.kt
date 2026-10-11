package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.behavior.tool.ToolTransform
import org.everbuild.blocksandstuff.blocks.behavior.tool.toolRule
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val waxableCopper: List<Block> =
    listOf(
        Block.COPPER_BLOCK,
        Block.EXPOSED_COPPER,
        Block.WEATHERED_COPPER,
        Block.OXIDIZED_COPPER,
        Block.CUT_COPPER,
        Block.EXPOSED_CUT_COPPER,
        Block.WEATHERED_CUT_COPPER,
        Block.OXIDIZED_CUT_COPPER,
        Block.CUT_COPPER_STAIRS,
        Block.EXPOSED_CUT_COPPER_STAIRS,
        Block.WEATHERED_CUT_COPPER_STAIRS,
        Block.OXIDIZED_CUT_COPPER_STAIRS,
        Block.CUT_COPPER_SLAB,
        Block.EXPOSED_CUT_COPPER_SLAB,
        Block.WEATHERED_CUT_COPPER_SLAB,
        Block.OXIDIZED_CUT_COPPER_SLAB,
        Block.CHISELED_COPPER,
        Block.EXPOSED_CHISELED_COPPER,
        Block.WEATHERED_CHISELED_COPPER,
        Block.OXIDIZED_CHISELED_COPPER,
        Block.COPPER_GRATE,
        Block.EXPOSED_COPPER_GRATE,
        Block.WEATHERED_COPPER_GRATE,
        Block.OXIDIZED_COPPER_GRATE,
        Block.COPPER_BULB,
        Block.EXPOSED_COPPER_BULB,
        Block.WEATHERED_COPPER_BULB,
        Block.OXIDIZED_COPPER_BULB,
        Block.COPPER_DOOR,
        Block.EXPOSED_COPPER_DOOR,
        Block.WEATHERED_COPPER_DOOR,
        Block.OXIDIZED_COPPER_DOOR,
        Block.COPPER_TRAPDOOR,
        Block.EXPOSED_COPPER_TRAPDOOR,
        Block.WEATHERED_COPPER_TRAPDOOR,
        Block.OXIDIZED_COPPER_TRAPDOOR,
        Block.COPPER_BARS,
        Block.EXPOSED_COPPER_BARS,
        Block.WEATHERED_COPPER_BARS,
        Block.OXIDIZED_COPPER_BARS,
        Block.COPPER_CHAIN,
        Block.EXPOSED_COPPER_CHAIN,
        Block.WEATHERED_COPPER_CHAIN,
        Block.OXIDIZED_COPPER_CHAIN,
        Block.COPPER_LANTERN,
        Block.EXPOSED_COPPER_LANTERN,
        Block.WEATHERED_COPPER_LANTERN,
        Block.OXIDIZED_COPPER_LANTERN,
        Block.COPPER_GOLEM_STATUE,
        Block.EXPOSED_COPPER_GOLEM_STATUE,
        Block.WEATHERED_COPPER_GOLEM_STATUE,
        Block.OXIDIZED_COPPER_GOLEM_STATUE,
        Block.LIGHTNING_ROD,
        Block.EXPOSED_LIGHTNING_ROD,
        Block.WEATHERED_LIGHTNING_ROD,
        Block.OXIDIZED_LIGHTNING_ROD,
    )

private val waxingTransforms: List<ToolTransform> =
    waxableCopper.mapNotNull { block ->
        val waxedKey = block.key().asString().replaceFirst(":", ":waxed_")
        Block.fromKey(waxedKey)?.let {
            ToolTransform(
                block,
                Material.HONEYCOMB,
                it,
                preserveProperties = true,
            )
        }
    }

val WAXING_RULE: Rule<InteractionActivityContext> = toolRule("blocksandstuff:waxing", waxingTransforms)

fun BlockRulesetEditor.addWaxing() {
    waxableCopper.forEach { block ->
        addBlock(block) { interact.append { install(WAXING_RULE) } }
    }
}
