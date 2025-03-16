package org.everbuild.blocksandstuff.common.tag

import net.minestom.server.instance.block.Block

object BlockTags : AggregatingTagProvider<Block>() {
    init {
        addChild(BlockRegistryTagProvider)
        addChild(MinestomBlockTagProvider)
    }
}