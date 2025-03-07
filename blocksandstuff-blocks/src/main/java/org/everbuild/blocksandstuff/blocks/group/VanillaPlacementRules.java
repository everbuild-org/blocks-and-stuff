package org.everbuild.blocksandstuff.blocks.group;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.everbuild.blocksandstuff.blocks.group.block.AggreateTagBlockGroup;
import org.everbuild.blocksandstuff.blocks.group.block.BlockBlockGroup;
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup;
import org.everbuild.blocksandstuff.blocks.group.block.VanillaTagBlockGroup;
import org.everbuild.blocksandstuff.blocks.group.placement.PlacementGroup;
import org.everbuild.blocksandstuff.blocks.placement.RotatedPillarPlacementRule;

import java.util.ArrayList;
import java.util.function.Function;

public class VanillaPlacementRules {
    public static final ArrayList<PlacementGroup> ALL = new ArrayList<>();

    public static final PlacementGroup ROTATED_PILLARS = group(
            all(
                    byTag("minecraft:logs"),
                    byBlock(Block.MUDDY_MANGROVE_ROOTS),
                    byBlock(Block.BAMBOO_BLOCK),
                    byBlock(Block.STRIPPED_BAMBOO_BLOCK),
                    byBlock(Block.BASALT),
                    byBlock(Block.POLISHED_BASALT),
                    byBlock(Block.QUARTZ_PILLAR),
                    byBlock(Block.PURPUR_PILLAR),
                    byBlock(Block.BONE_BLOCK),
                    byBlock(Block.DEEPSLATE),
                    byBlock(Block.INFESTED_DEEPSLATE),
                    byBlock(Block.OCHRE_FROGLIGHT),
                    byBlock(Block.VERDANT_FROGLIGHT),
                    byBlock(Block.PEARLESCENT_FROGLIGHT)
            ),
            RotatedPillarPlacementRule::new
    );

    private static PlacementGroup group(BlockGroup blockGroup, Function<Block, BlockPlacementRule> valueFunction) {
        PlacementGroup result = new PlacementGroup(blockGroup, valueFunction);
        ALL.add(result);
        return result;
    }

    private static BlockGroup all(BlockGroup... blockGroups) {
        return new AggreateTagBlockGroup(blockGroups);
    }

    private static BlockGroup byTag(@KeyPattern String tag) {
        return new VanillaTagBlockGroup(Key.key(tag));
    }

    private static BlockGroup byBlock(Block block) {
        return new BlockBlockGroup(block);
    }
}
