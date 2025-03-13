package org.everbuild.blocksandstuff.blocks.group.block

import net.minestom.server.instance.block.Block

class BlockBlockGroup(private val block: Block) : BlockGroup {
    override fun allMatching(): Collection<Block> {
        return listOf(block)
    }
}
