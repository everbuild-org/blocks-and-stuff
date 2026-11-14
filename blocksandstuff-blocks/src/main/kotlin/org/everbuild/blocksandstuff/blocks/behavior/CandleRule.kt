package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.tool.damageTool
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val candles: Collection<Block> = BlockTags.getTaggedWith("minecraft:candles")

val CANDLE_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:candle") { ctx ->
        val block = ctx.block
        val lit = block.getProperty("lit") ?: return@Rule RuleResult.Next
        val waterlogged = block.getProperty("waterlogged") == "true"
        val item = ctx.player.getItemInHand(ctx.hand)
        val otherHand = if (ctx.hand == PlayerHand.MAIN) PlayerHand.OFF else PlayerHand.MAIN
        val otherItem = ctx.player.getItemInHand(otherHand)

        when {
            lit != "true" && !waterlogged && isLighter(item) -> {
                ctx.replaceBlock(block.withProperty("lit", "true"))
                ctx.instance.playSoundExcept(
                    ctx.player,
                    Sound.sound(lighterSound(item), Sound.Source.BLOCK, 1.0f, 1.0f),
                    ctx.blockPosition,
                )
                consumeLighter(ctx.player, ctx.hand, item)
                RuleResult.Consume
            }

            lit == "true" && item.isAir && !isLighter(otherItem) -> {
                ctx.replaceBlock(block.withProperty("lit", "false"))
                ctx.instance.playSoundExcept(
                    ctx.player,
                    Sound.sound(SoundEvent.BLOCK_CANDLE_EXTINGUISH, Sound.Source.BLOCK, 1.0f, 1.0f),
                    ctx.blockPosition,
                )
                RuleResult.Consume
            }

            else -> {
                RuleResult.Next
            }
        }
    }

fun isLighter(item: ItemStack): Boolean = item.material() == Material.FLINT_AND_STEEL || item.material() == Material.FIRE_CHARGE

fun lighterSound(item: ItemStack): SoundEvent =
    if (item.material() == Material.FIRE_CHARGE) SoundEvent.ITEM_FIRECHARGE_USE else SoundEvent.ITEM_FLINTANDSTEEL_USE

fun consumeLighter(
    player: Player,
    hand: PlayerHand,
    item: ItemStack,
) {
    if (player.gameMode == GameMode.CREATIVE) return

    when (item.material()) {
        Material.FIRE_CHARGE -> player.setItemInHand(hand, item.withAmount(item.amount() - 1))
        Material.FLINT_AND_STEEL -> damageTool(player, item)
        else -> Unit
    }
}

fun BlockRulesetEditor.addCandles() {
    candles.forEach { block ->
        blocks(block) { interact.append { install(CANDLE_RULE) } }
    }
}
