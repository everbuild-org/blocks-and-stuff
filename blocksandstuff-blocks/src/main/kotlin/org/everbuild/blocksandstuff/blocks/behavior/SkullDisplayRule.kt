package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag

class SkullDisplayRule(block: Block) : BlockHandler {
    override fun getKey(): Key = Companion.key

    override fun getBlockEntityTags(): Collection<Tag<*>> {
        return listOf(
            Tag.NBT("profile"),
            Tag.String("note_block_sound"),
            Tag.NBT("custom_name")
        )
    }

    companion object {
        val key: Key = Key.key("minecraft:skull")
    }
}