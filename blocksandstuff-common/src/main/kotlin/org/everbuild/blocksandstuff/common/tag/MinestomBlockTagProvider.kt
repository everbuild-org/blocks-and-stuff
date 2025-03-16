package org.everbuild.blocksandstuff.common.tag

import net.minestom.server.MinecraftServer
import net.minestom.server.gamedata.tags.Tag
import net.minestom.server.instance.block.Block

object MinestomBlockTagProvider : TagProvider<Block> {
    private val tags = MinecraftServer.getTagManager()

    override fun hasTag(element: Block, tag: String): Boolean {
        val data = tags.getTag(Tag.BasicType.BLOCKS, tag) ?: return false
        return data.contains(element.namespace())
    }

    override fun getTaggedWith(tag: String): Set<Block> {
        val data = tags.getTag(Tag.BasicType.BLOCKS, tag) ?: return setOf()
        return data.values.mapNotNull(Block::fromNamespaceId).toSet()
    }
}