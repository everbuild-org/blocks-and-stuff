package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.block.Block
import net.minestom.server.registry.TagKey
import org.everbuild.blocksandstuff.blocks.event.CakeEatEvent
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private const val MAX_SLICES = 7
private val candles = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:candles"))!!

val CAKE_EAT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:cake_eat") { ctx ->
        if (ctx.player.isSneaking) return@Rule RuleResult.Next

        val currentSlices = ctx.block.getProperty("bites")!!.toInt()
        val heldBlock = ctx.player.itemInMainHand.material().block() ?: Block.AIR
        if (currentSlices == 0 && candles.contains(heldBlock)) return@Rule RuleResult.Next

        EventDispatcher.callCancellable(
            CakeEatEvent(ctx.player, ctx.block, ctx.blockPosition.asBlockVec()),
        ) {
            if (currentSlices < MAX_SLICES - 1) {
                ctx.instance.setBlock(
                    ctx.blockPosition,
                    ctx.block.withProperty("bites", (currentSlices + 1).toString()),
                )
            } else {
                ctx.instance.setBlock(ctx.blockPosition, Block.AIR)
            }
        }

        RuleResult.Consume
    }

fun BlockRulesetEditor.addCakes() {
    blocks(Block.CAKE) { interact.append { install(CAKE_EAT_RULE) } }
}
