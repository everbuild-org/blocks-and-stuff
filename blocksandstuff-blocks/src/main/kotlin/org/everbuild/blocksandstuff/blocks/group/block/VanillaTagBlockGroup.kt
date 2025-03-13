package org.everbuild.blocksandstuff.blocks.group.block

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.gamedata.tags.Tag
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.NamespaceID

class VanillaTagBlockGroup(private val key: Key) : BlockGroup {
    override fun allMatching(): Collection<Block> {
        val tag = MinecraftServer
            .getTagManager()
            .getTag(Tag.BasicType.BLOCKS, key.asString())!!
        return tag
            .values
            .map(NamespaceID::from)
            .mapNotNull(Block::fromNamespaceId)
            .toList()
    }
}
