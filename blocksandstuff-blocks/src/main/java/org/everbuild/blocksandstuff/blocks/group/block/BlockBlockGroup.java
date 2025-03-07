package org.everbuild.blocksandstuff.blocks.group.block;

import net.minestom.server.instance.block.Block;

import java.util.Collection;
import java.util.List;

public class BlockBlockGroup implements BlockGroup {
    private final Block block;

    public BlockBlockGroup(Block block) {
        this.block = block;
    }

    @Override
    public Collection<Block> allMatching() {
        return List.of(block);
    }
}
