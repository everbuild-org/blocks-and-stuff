package org.everbuild.blocksandstuff.blocks.group.block

import java.util.Arrays
import net.minestom.server.instance.block.Block

class AggregateTagBlockGroup(private vararg val children: BlockGroup) : BlockGroup {

    override fun allMatching(): Collection<Block> {
        return Arrays.stream(children).flatMap { child: BlockGroup -> child.allMatching().stream() }.toList()
    }
}
