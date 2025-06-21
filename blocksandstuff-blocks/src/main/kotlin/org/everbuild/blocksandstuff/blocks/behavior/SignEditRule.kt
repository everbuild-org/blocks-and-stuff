package org.everbuild.blocksandstuff.blocks.behavior

import kotlin.math.atan2
import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.network.packet.server.play.OpenSignEditorPacket
import net.minestom.server.registry.TagKey
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.blocks.event.PlayerOpenSignEditorEvent

class SignEditRule(private val block: Block) : BlockHandler {
    private val wallSigns = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:wall_signs"))!!

    override fun getKey(): Key = block.key()

    override fun getBlockEntityTags(): Collection<Tag<*>> {
        return listOf(
            Tag.NBT("front_text"),
            Tag.NBT("back_text"),
            Tag.Boolean("is_waxed"),
        )
    }

    override fun onPlace(placement: BlockHandler.Placement) {
        println("SignEditRule: onPlace")
        if (placement !is BlockHandler.PlayerPlacement) return
        if (placement.player.isSneaking) return
        val player = placement.player
        if (wallSigns.contains(placement.block)) {
            openEditor(placement.block, placement.blockPosition, player, true)
        } else {
            val position = placement.blockPosition
            val rotation = placement.block.getProperty("rotation")?.toInt() ?: 0
            val front = getSide(player, position, rotation)
            openEditor(placement.block, position, player, front)
        }
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        println("SignEditRule: onInteract")
        if (interaction.player.isSneaking) return super.onInteract(interaction)
        val block = interaction.block
        val position = interaction.blockPosition
        val rotation = block.getProperty("rotation")?.toInt() ?: 0
        val front = getSide(interaction.player, position, rotation)
        if (wallSigns.contains(block)) {
            openEditor(block, position, interaction.player, front)
        } else {
            val rotation = block.getProperty("rotation")?.toInt() ?: 0
            val side = getSide(interaction.player, position, rotation)
            openEditor(block, position, interaction.player, side)
        }

        return false
    }

    private fun getSide(player: Player, position: Point, rotation: Int): Boolean {
        val playerAngle = Math.toDegrees(
            atan2(player.position.x - position.x(), position.z() - player.position.z)
        ).toInt()
        val signAngle = rotation * 22.5
        val relativeDegrees = (playerAngle - signAngle + 360) % 360
        return relativeDegrees in 0.0..180.0
    }

    private fun openEditor(block: Block, position: Point, player: Player, front: Boolean) {
        EventDispatcher.callCancellable(PlayerOpenSignEditorEvent(player, BlockVec(position), block)) {
            player.sendPacket(OpenSignEditorPacket(position, front))
        }
    }

    // TODO: Write back updated signs
}