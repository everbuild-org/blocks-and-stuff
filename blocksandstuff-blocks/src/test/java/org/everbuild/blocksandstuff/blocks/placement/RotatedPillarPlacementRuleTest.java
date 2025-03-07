package org.everbuild.blocksandstuff.blocks.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class RotatedPillarPlacementRuleTest {
    private void assetBlockPlacement(BlockFace blockFace, String expectedAxis) {
        BlockPlacementRule rule = new RotatedPillarPlacementRule(Block.OAK_LOG);

        Block result = rule.blockPlace(
                new BlockPlacementRule.PlacementState(
                        Mockito.mock(Instance.class),
                        Block.OAK_LOG,
                        blockFace,
                        new Pos(0, 0, 0),
                        new Pos(0, 0, 0),
                        new Pos(0, 0, 0),
                        null,
                        false
                )
        );


        assertNotNull(result);
        assertEquals(expectedAxis, result.getProperty("axis"));
    }

    @Test
    void blockPlace() {
        assetBlockPlacement(BlockFace.EAST, "x");
        assetBlockPlacement(BlockFace.WEST, "x");
        assetBlockPlacement(BlockFace.NORTH, "z");
        assetBlockPlacement(BlockFace.SOUTH, "z");
        assetBlockPlacement(BlockFace.BOTTOM, "y");
        assetBlockPlacement(BlockFace.TOP, "y");
    }
}