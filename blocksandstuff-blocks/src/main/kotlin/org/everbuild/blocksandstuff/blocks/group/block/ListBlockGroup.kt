package org.everbuild.blocksandstuff.blocks.group.block

import net.minestom.server.instance.block.Block

class ListBlockGroup(val all: Collection<Block>) : BlockGroup {
    override fun allMatching(): Collection<Block> = all
}