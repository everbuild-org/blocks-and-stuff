package org.everbuild.blocksandstuff.blocks.group.block

import net.minestom.server.instance.block.Block

class ExcludeBlockRule(val blocks: BlockGroup, val excludedBlocks: BlockGroup) : BlockGroup {
    override fun allMatching(): Collection<Block> {
        val excludedMatching = excludedBlocks.allMatching()
        return blocks.allMatching().filter { block -> !excludedMatching.contains(block) }
    }
}