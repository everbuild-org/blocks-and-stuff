package org.everbuild.blocksandstuff.blocks.group.block;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface BlockGroup extends IntoBlockGroup {
    Collection<Block> allMatching();
    @Override
    default @NotNull BlockGroup getBlockGroup() {
        return this;
    }
}
