package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Transcoder
import net.minestom.server.event.player.PlayerEditSignEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.SignTextSlot
import net.minestom.server.tag.Tag

object SignEditListener {
    private val frontTextTag: Tag<BinaryTag> = Tag.NBT("front_text")
    private val backTextTag: Tag<BinaryTag> = Tag.NBT("back_text")

    fun enable() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerEditSignEvent::class.java) { event ->
            val tag = if (event.textSlot == SignTextSlot.FRONT) frontTextTag else backTextTag
            val text = buildText(event.block, tag, event.lines)
            event.instance.setBlock(event.blockPosition, event.block.withTag(tag, text))
        }
    }

    private fun buildText(
        block: Block,
        tag: Tag<BinaryTag>,
        lines: List<String>,
    ): BinaryTag {
        val existing = block.getTag(tag) as? CompoundBinaryTag
        val messageTags: List<BinaryTag> =
            lines.map { line ->
                Codec.COMPONENT.encode(Transcoder.NBT, Component.text(line)).orElseThrow()
            }
        return CompoundBinaryTag
            .builder()
            .put("messages", ListBinaryTag.from(messageTags))
            .putString("color", existing?.getString("color", "black") ?: "black")
            .putBoolean("has_glowing_text", existing?.getBoolean("has_glowing_text", false) ?: false)
            .build()
    }
}
