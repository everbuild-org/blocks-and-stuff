package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.ToolTransform
import org.everbuild.blocksandstuff.blocks.behavior.tool.damageTool
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.blocks.behavior.tool.toolRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val shearsTransforms: List<ToolTransform> =
    listOf(
        ToolTransform(
            Block.PUMPKIN,
            Material.SHEARS,
            Block.CARVED_PUMPKIN,
            preserveProperties = true,
            damagesTool = true,
            extra = { ctx, _ -> playSoundFor(ctx, SoundEvent.BLOCK_PUMPKIN_CARVE) },
        ),
    )

val SHEARS_RULE: Rule<InteractionActivityContext> = toolRule("blocksandstuff:shears", shearsTransforms)

val BEEHIVE_SHEAR_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:beehive_shear") { ctx ->
        if (ctx.player.itemInMainHand.material() != Material.SHEARS) return@Rule RuleResult.Next
        if (ctx.block.getProperty("honey_level")?.toIntOrNull() != 5) return@Rule RuleResult.Next

        repeat(3) {
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ItemStack.of(Material.HONEYCOMB))
        }
        ctx.replaceBlock(ctx.block.withProperty("honey_level", "0"))
        playSoundFor(ctx, SoundEvent.BLOCK_BEEHIVE_SHEAR)
        damageTool(ctx.player, ctx.player.itemInMainHand)
        RuleResult.Consume
    }

private val shearDroppableVanillaBlocks: List<Block> =
    listOf(
        Block.VINE,
        Block.CAVE_VINES,
        Block.CAVE_VINES_PLANT,
        Block.GLOW_LICHEN,
        Block.HANGING_ROOTS,
        Block.TWISTING_VINES,
        Block.TWISTING_VINES_PLANT,
        Block.WEEPING_VINES,
        Block.WEEPING_VINES_PLANT,
    )

val SHEARS_DROP_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:shears_drop") { ctx ->
        val player = ctx.player ?: return@Rule RuleResult.Next
        if (player.itemInMainHand.material() != Material.SHEARS) return@Rule RuleResult.Next
        if (ctx.newBlock.key() == ctx.block.key()) return@Rule RuleResult.Next

        DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, ctx.block)
        damageTool(player, player.itemInMainHand)
        RuleResult.Next
    }

fun BlockRulesetEditor.addShears() {
    blocks(Block.PUMPKIN) { interact.append { install(SHEARS_RULE) } }
    listOf(Block.BEE_NEST, Block.BEEHIVE).forEach { block ->
        blocks(block) { interact.append { install(BEEHIVE_SHEAR_RULE) } }
    }
    shearDroppableVanillaBlocks.forEach { block ->
        blocks(block) { destroy.append { install(SHEARS_DROP_RULE) } }
    }
}
