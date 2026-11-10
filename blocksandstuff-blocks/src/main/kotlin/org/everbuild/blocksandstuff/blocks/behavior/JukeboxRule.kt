package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.GameMode
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

private val JUKEBOX_RECORD_TAG: Tag<ItemStack> = Tag.ItemStack("RecordItem")

val JUKEBOX_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:jukebox") { ctx ->
        val stored = ctx.block.getTag(JUKEBOX_RECORD_TAG)
        val held = ctx.player.itemInMainHand
        val heldSong = held.get(DataComponents.JUKEBOX_PLAYABLE)

        when {
            stored == null -> {
                if (heldSong == null) return@Rule RuleResult.Next
                if (!playRecord(ctx, held, heldSong)) return@Rule RuleResult.Next
                consumeHeldRecord(ctx, held)
                RuleResult.Consume
            }

            heldSong != null -> {
                if (!playRecord(ctx, held, heldSong)) return@Rule RuleResult.Next
                stopJukeboxSound(ctx.instance, stored)
                DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, stored)
                consumeHeldRecord(ctx, held)
                RuleResult.Consume
            }

            held.isAir -> {
                stopJukeboxSound(ctx.instance, stored)
                DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, stored)
                ctx.instance.setBlock(
                    ctx.blockPosition,
                    ctx.block.withTag(JUKEBOX_RECORD_TAG, null).withProperty("has_record", "false"),
                    false,
                )
                RuleResult.Consume
            }

            else -> RuleResult.Next
        }
    }

private fun playRecord(
    ctx: InteractionActivityContext,
    record: ItemStack,
    songKey: net.minestom.server.registry.RegistryKey<net.minestom.server.instance.block.jukebox.JukeboxSong>,
): Boolean {
    val song = MinecraftServer.getJukeboxSongRegistry().get(songKey) ?: return false
    ctx.instance.setBlock(
        ctx.blockPosition,
        ctx.block.withTag(JUKEBOX_RECORD_TAG, record.withAmount(1)).withProperty("has_record", "true"),
        false,
    )
    ctx.instance.playSound(
        Sound.sound(song.soundEvent(), Sound.Source.RECORD, 1.0f, 1.0f),
        ctx.blockPosition,
    )
    return true
}

private fun consumeHeldRecord(
    ctx: InteractionActivityContext,
    held: ItemStack,
) {
    if (ctx.player.gameMode == GameMode.CREATIVE) return
    ctx.player.itemInMainHand = if (held.amount() <= 1) ItemStack.AIR else held.withAmount(held.amount() - 1)
}

val JUKEBOX_DESTROY_RULE: Rule<DestroyActivityContext> =
    Rule("blocksandstuff:jukebox_destroy") { ctx ->
        if (ctx.newBlock.key() == ctx.block.key()) return@Rule RuleResult.Next

        val stored = ctx.block.getTag(JUKEBOX_RECORD_TAG) ?: return@Rule RuleResult.Next
        stopJukeboxSound(ctx.instance, stored)
        DroppedItemFactory.maybeDrop(ctx.instance, ctx.blockPosition, stored)
        RuleResult.Next
    }

private fun stopJukeboxSound(
    instance: Instance,
    record: ItemStack,
) {
    val songKey = record.get(DataComponents.JUKEBOX_PLAYABLE) ?: return
    val soundEvent = MinecraftServer.getJukeboxSongRegistry().get(songKey)?.soundEvent() ?: return
    instance.stopSound(SoundStop.namedOnSource(soundEvent.key(), Sound.Source.RECORD))
}

fun BlockRulesetEditor.addJukebox() {
    blocks(Block.JUKEBOX) {
        interact.append { install(JUKEBOX_RULE) }
        destroy.append { install(JUKEBOX_DESTROY_RULE) }
    }
}
