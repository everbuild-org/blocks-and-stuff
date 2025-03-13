package org.everbuild.blocksandstuff.blocks.group.block

import net.minestom.server.instance.block.Block

interface BlockGroup : IntoBlockGroup {
    fun allMatching(): Collection<Block>
    override val blockGroup: BlockGroup
        get() = this
}
