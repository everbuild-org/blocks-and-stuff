package org.everbuild.blocksandstuff.blocks.group.block;

import net.minestom.server.instance.block.Block;

import java.util.Arrays;
import java.util.Collection;

public class AggreateTagBlockGroup implements BlockGroup {
    private final BlockGroup[] children;

    public AggreateTagBlockGroup(BlockGroup... children) {
        this.children = children;
    }

    @Override
    public Collection<Block> allMatching() {
        return Arrays.stream(children).flatMap(child -> child.allMatching().stream()).toList();
    }
}
