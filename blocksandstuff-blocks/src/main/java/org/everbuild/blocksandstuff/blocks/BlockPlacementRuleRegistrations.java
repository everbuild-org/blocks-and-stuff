package org.everbuild.blocksandstuff.blocks;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockManager;
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup;
import org.everbuild.blocksandstuff.blocks.group.placement.PlacementGroup;
import org.everbuild.blocksandstuff.blocks.group.VanillaPlacementRules;

public class BlockPlacementRuleRegistrations {
    public static void registerDefault() {
        register(VanillaPlacementRules.ALL.toArray(new PlacementGroup[0]));
    }

    public static void register(PlacementGroup... blockGroups) {
        BlockManager blockManager = MinecraftServer.getBlockManager();

        for (PlacementGroup placementGroup : blockGroups) {
            BlockGroup blockGroup = placementGroup.getBlockGroup();
            for (Block block : blockGroup.allMatching()) {
                blockManager.registerBlockPlacementRule(placementGroup.createRule(block));
            }
        }
    }
}
