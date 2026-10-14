package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.instance.block.Block
import net.minestom.server.sound.SoundEvent
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

val LEVER_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:lever") { ctx ->
        if (ctx.player.isSneaking && !ctx.player.itemInMainHand.isAir) return@Rule RuleResult.Next

        val powered = ctx.block.getProperty("powered") == "true"
        ctx.replaceBlock(ctx.block.withProperty("powered", (!powered).toString()))

        ctx.instance.playSoundExcept(
            null,
            Sound.sound(SoundEvent.BLOCK_LEVER_CLICK, Sound.Source.BLOCK, 0.3f, if (powered) 0.6f else 0.5f),
            ctx.blockPosition,
        )

        RuleResult.Consume
    }

fun BlockRulesetEditor.addLevers() {
    blocks(Block.LEVER) { interact.append { install(LEVER_RULE) } }
}
