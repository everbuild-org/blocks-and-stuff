package org.everbuild.blocksandstuff.blocks.behavior

import kotlin.math.abs
import kotlin.math.atan2
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.SignTextSlot
import net.minestom.server.network.packet.server.play.OpenSignEditorPacket
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.blocks.event.PlayerOpenSignEditorEvent
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.common.utils.getYaw
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour_block.configuration.blockEntity
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.PlacementActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact
import org.everbuild.twaddle.stacked_behaviour_block.type.place

private val signBlocks: Collection<Block> = BlockTags.getTaggedWith("minecraft:all_signs")

val SIGN_INTERACT_RULE: Rule<InteractionActivityContext> =
    Rule("blocksandstuff:sign_interact") { ctx ->
        if (ctx.player.isSneaking) return@Rule RuleResult.Next
        val side = getSide(ctx.player, ctx.blockPosition, ctx.block)
        openEditor(ctx.block, ctx.blockPosition, ctx.player, side)
        RuleResult.Consume
    }

val SIGN_PLACE_RULE: Rule<PlacementActivityContext> =
    Rule("blocksandstuff:sign_place") { ctx ->
        val player = ctx.player ?: return@Rule RuleResult.Next
        if (player.isSneaking) return@Rule RuleResult.Next
        val slot = getSide(player, ctx.blockPosition, ctx.block)
        openEditor(ctx.block, ctx.blockPosition, player, slot)
        RuleResult.Next
    }

private fun getSide(
    player: Player,
    position: Point,
    block: Block,
): SignTextSlot {
    val signYaw =
        block.getProperty("rotation")?.toIntOrNull()?.times(22.5)
            ?: block.getProperty("facing")
                ?.let { BlockFace.valueOf(it.uppercase()).toDirection().getYaw().toDouble() }
            ?: 0.0
    val dx = player.position.x - (position.x() + 0.5)
    val dz = player.position.z - (position.z() + 0.5)
    val playerAngle = Math.toDegrees(atan2(dz, dx)) - 90.0
    val difference = ((signYaw - playerAngle + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
    return if (abs(difference) <= 90.0) SignTextSlot.FRONT else SignTextSlot.BACK
}

private fun openEditor(
    block: Block,
    position: Point,
    player: Player,
    slot: SignTextSlot,
) {
    EventDispatcher.callCancellable(PlayerOpenSignEditorEvent(player, position.asBlockVec(), block)) {
        player.sendPacket(OpenSignEditorPacket(position, slot))
    }
}

fun BlockRulesetEditor.addSigns() {
    signBlocks.forEach { block ->
        blocks(block) {
            blockEntity {
                tags += Tag.NBT("front_text")
                tags += Tag.NBT("back_text")
                tags += Tag.Boolean("is_waxed")
            }
            interact.append { install(SIGN_INTERACT_RULE) }
            place.append { install(SIGN_PLACE_RULE) }
        }
    }
}
