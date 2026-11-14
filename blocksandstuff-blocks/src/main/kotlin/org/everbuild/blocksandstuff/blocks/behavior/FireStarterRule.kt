package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.entity.GameMode
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.damageTool
import org.everbuild.blocksandstuff.blocks.behavior.tool.playSoundFor
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private fun isLighter(material: Material): Boolean =
    material == Material.FLINT_AND_STEEL || material == Material.FIRE_CHARGE

val CAMPFIRE_LIGHT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:campfire_light") { ctx ->
        val held = ctx.player.itemInMainHand
        if (!isLighter(held.material())) return@Rule RuleResult.Next
        if (ctx.block.getProperty("lit") == "true") return@Rule RuleResult.Next

        ctx.replaceBlock(ctx.block.withProperty("lit", "true"))
        playSoundFor(
            ctx,
            if (held.material() == Material.FIRE_CHARGE) SoundEvent.ITEM_FIRECHARGE_USE else SoundEvent.ITEM_FLINTANDSTEEL_USE,
        )
        if (held.material() == Material.FLINT_AND_STEEL) {
            damageTool(ctx.player, held)
        } else if (ctx.player.gameMode != GameMode.CREATIVE) {
            ctx.player.itemInMainHand = held.withAmount(held.amount() - 1)
        }
        RuleResult.Consume
    }

fun BlockRulesetEditor.addFireStarters() {
    listOf(Block.CAMPFIRE, Block.SOUL_CAMPFIRE).forEach { block ->
        blocks(block) { interact.append { install(CAMPFIRE_LIGHT_RULE) } }
    }
}
