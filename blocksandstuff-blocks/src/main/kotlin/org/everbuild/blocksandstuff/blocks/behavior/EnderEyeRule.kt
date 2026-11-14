package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.entity.GameMode
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

val END_PORTAL_FRAME_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:end_portal_frame_eye") { ctx ->
        if (!ctx.block.compare(Block.END_PORTAL_FRAME)) return@Rule RuleResult.Next
        if (ctx.block.getProperty("eye") == "true") return@Rule RuleResult.Next

        val held = ctx.player.itemInMainHand
        if (held.material() != Material.ENDER_EYE) return@Rule RuleResult.Next

        ctx.instance.setBlock(ctx.blockPosition, ctx.block.withProperty("eye", "true"))
        if (ctx.player.gameMode != GameMode.CREATIVE) {
            ctx.player.itemInMainHand = if (held.amount() <= 1) ItemStack.AIR else held.withAmount(held.amount() - 1)
        }
        playSoundFor(ctx, SoundEvent.BLOCK_END_PORTAL_FRAME_FILL)
        RuleResult.Consume
    }

fun BlockRulesetEditor.addEnderEye() {
    blocks(Block.END_PORTAL_FRAME) { interact.append { install(END_PORTAL_FRAME_RULE) } }
}
