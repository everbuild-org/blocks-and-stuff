package org.everbuild.blocksandstuff.blocks.group.block

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.tag.BlockTags

class TagBlockGroup(private val key: Key) : BlockGroup {
    override fun allMatching(): Collection<Block> {
        return BlockTags.getTaggedWith(key.asString())
    }
}
