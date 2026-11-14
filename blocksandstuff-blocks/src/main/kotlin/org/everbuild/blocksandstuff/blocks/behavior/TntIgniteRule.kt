package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.util.PrimedTnt
import org.everbuild.blocksandstuff.redstone.VibrationBus
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

val TNT_IGNITE_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:tnt_ignite") { ctx ->
        val item = ctx.player.getItemInHand(ctx.hand)
        if (!isLighter(item)) return@Rule RuleResult.Next

        PrimedTnt.spawn(ctx.instance, ctx.blockPosition)
        ctx.instance.setBlock(ctx.blockPosition, Block.AIR)
        ctx.instance.playSoundExcept(
            ctx.player,
            Sound.sound(lighterSound(item), Sound.Source.BLOCK, 1.0f, 1.0f),
            ctx.blockPosition,
        )
        consumeLighter(ctx.player, ctx.hand, item)
        VibrationBus.emit(ctx.instance, ctx.blockPosition, 10)
        RuleResult.Consume
    }

fun BlockRulesetEditor.addTntIgnition() {
    blocks(Block.TNT) { interact.append { install(TNT_IGNITE_RULE) } }
}
