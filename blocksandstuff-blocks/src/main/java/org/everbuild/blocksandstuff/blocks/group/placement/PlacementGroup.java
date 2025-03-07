package org.everbuild.blocksandstuff.blocks.group.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup;
import org.everbuild.blocksandstuff.blocks.group.block.IntoBlockGroup;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class PlacementGroup implements IntoBlockGroup, IntoPlacementRule {
    private final BlockGroup blockGroup;
    private final Function<Block, BlockPlacementRule> valueFunction;

    public PlacementGroup(BlockGroup blockGroup, Function<Block, BlockPlacementRule> valueFunction) {
        this.blockGroup = blockGroup;
        this.valueFunction = valueFunction;
    }

    @Override
    public @NotNull BlockGroup getBlockGroup() {
        return blockGroup;
    }

    @Override
    public BlockPlacementRule createRule(Block block) {
        return valueFunction.apply(block);
    }
}
