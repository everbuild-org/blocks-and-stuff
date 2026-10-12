package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.event.CakeEatEvent
import org.everbuild.blocksandstuff.blocks.placement.CandlePlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val candleCakes: Collection<Block> = BlockTags.getTaggedWith("minecraft:candle_cakes")
private val candleForCakeCake =
    CandlePlacementRule.CANDLE_CAKE.entries.associate { (candle, cakeCake) -> cakeCake to candle }

val CANDLE_CAKE_EAT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:candle_cake_eat") { ctx ->
        if (ctx.player.isSneaking) return@Rule RuleResult.Next

        EventDispatcher.callCancellable(
            CakeEatEvent(ctx.player, ctx.block, ctx.blockPosition.asBlockVec()),
        ) {
            val candle = candleForCakeCake[ctx.block.defaultState()] ?: return@callCancellable
            ctx.instance.setBlock(
                ctx.blockPosition,
                Block.CAKE.withDefaultHandler().withProperty("bites", "1"),
            )
            DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, candle)
        }

        RuleResult.Consume
    }

fun BlockRulesetEditor.addCandleCakes() {
    candleCakes.forEach { block ->
        blocks(block) { interact.append { install(CANDLE_CAKE_EAT_RULE) } }
    }
}
