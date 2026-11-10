package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.entity.GameMode
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val DECORATED_POT_ITEM_TAG: Tag<ItemStack> = Tag.ItemStack("item")

val DECORATED_POT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:decorated_pot") { ctx ->
        val stored = ctx.block.getTag(DECORATED_POT_ITEM_TAG)
        val held = ctx.player.itemInMainHand

        if (stored == null && !held.isAir) {
            ctx.instance.setBlock(ctx.blockPosition, ctx.block.withTag(DECORATED_POT_ITEM_TAG, held.withAmount(1)), false)
            playSoundFor(ctx, SoundEvent.BLOCK_DECORATED_POT_INSERT)
            if (ctx.player.gameMode != GameMode.CREATIVE) {
                ctx.player.itemInMainHand =
                    if (held.amount() <= 1) ItemStack.AIR else held.withAmount(held.amount() - 1)
            }
            return@Rule RuleResult.Consume
        }

        if (stored != null && held.isAir) {
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, stored)
            ctx.instance.setBlock(ctx.blockPosition, ctx.block.withTag(DECORATED_POT_ITEM_TAG, null), false)
            playSoundFor(ctx, SoundEvent.BLOCK_DECORATED_POT_INSERT)
            return@Rule RuleResult.Consume
        }

        RuleResult.Next
    }

fun BlockRulesetEditor.addDecoratedPots() {
    blocks(Block.DECORATED_POT) { interact.append { install(DECORATED_POT_RULE) } }
}
