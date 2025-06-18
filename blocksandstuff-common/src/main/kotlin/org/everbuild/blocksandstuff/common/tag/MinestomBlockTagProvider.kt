package org.everbuild.blocksandstuff.common.tag

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block

object MinestomBlockTagProvider : TagProvider<Block> {
    override fun hasTag(element: Block, tag: String): Boolean {
        val data = Block.staticRegistry().getTag(Key.key(tag)) ?: return false
        return data.contains(element)
    }

    override fun getTaggedWith(tag: String): Set<Block> {
        val data = Block.staticRegistry().getTag(Key.key(tag))
        return data?.mapNotNull { Block.fromKey(it.key()) }?.toSet() ?: setOf()
    }
}