package org.everbuild.blocksandstuff.blocks.group.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;

public interface IntoPlacementRule {
    BlockPlacementRule createRule(Block block);
}
