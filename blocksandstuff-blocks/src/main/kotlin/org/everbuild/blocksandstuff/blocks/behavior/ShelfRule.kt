package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.entity.GameMode
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.blocks.util.readBlockEntityItems
import org.everbuild.blocksandstuff.blocks.util.withBlockEntityItems
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

internal val SHELF_BLOCKS: List<Block> =
    listOf(
        Block.OAK_SHELF,
        Block.SPRUCE_SHELF,
        Block.BIRCH_SHELF,
        Block.JUNGLE_SHELF,
        Block.ACACIA_SHELF,
        Block.DARK_OAK_SHELF,
        Block.MANGROVE_SHELF,
        Block.CHERRY_SHELF,
        Block.PALE_OAK_SHELF,
        Block.POPLAR_SHELF,
        Block.BAMBOO_SHELF,
        Block.CRIMSON_SHELF,
        Block.WARPED_SHELF,
    )

private const val SHELF_SLOTS = 3

val SHELF_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:shelf") { ctx ->
        if (SHELF_BLOCKS.none { it.key() == ctx.block.key() }) return@Rule RuleResult.Next
        val current = ctx.instance.getBlock(ctx.blockPosition)
        val items = current.readBlockEntityItems()
        val held = ctx.player.itemInMainHand

        if (held.isAir) {
            val slot = (SHELF_SLOTS - 1 downTo 0).firstOrNull { items.containsKey(it) } ?: return@Rule RuleResult.Next
            val stored = items.getValue(slot)
            ctx.instance.setBlock(ctx.blockPosition, current.withBlockEntityItems(items - slot))
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, stored)
            playSoundFor(ctx, SoundEvent.BLOCK_SHELF_TAKE_ITEM)
            return@Rule RuleResult.Consume
        }

        val slot = (0 until SHELF_SLOTS).firstOrNull { !items.containsKey(it) } ?: return@Rule RuleResult.Next
        ctx.instance.setBlock(ctx.blockPosition, current.withBlockEntityItems(items + (slot to held.withAmount(1))))
        if (ctx.player.gameMode != GameMode.CREATIVE) {
            ctx.player.itemInMainHand = if (held.amount() <= 1) ItemStack.AIR else held.withAmount(held.amount() - 1)
        }
        playSoundFor(ctx, SoundEvent.BLOCK_SHELF_PLACE_ITEM)
        RuleResult.Consume
    }

val SHELF_DESTROY_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:shelf_destroy") { ctx ->
        if (SHELF_BLOCKS.none { it.key() == ctx.block.key() }) return@Rule RuleResult.Next
        if (ctx.newBlock.key() == ctx.block.key()) return@Rule RuleResult.Next

        ctx.block.readBlockEntityItems().values.forEach {
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, it)
        }
        RuleResult.Next
    }

fun BlockRulesetEditor.addShelves() {
    SHELF_BLOCKS.forEach { shelf ->
        blocks(shelf) {
            interact.append { install(SHELF_RULE) }
            destroy.append { install(SHELF_DESTROY_RULE) }
        }
    }
}
