package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.instance.block.rule.BlockPlacementRule.PlacementState
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

internal class RotatedPillarPlacementRuleTest {
    private fun assetBlockPlacement(blockFace: BlockFace, expectedAxis: String) {
        val rule: BlockPlacementRule = RotatedPillarPlacementRule(Block.OAK_LOG)

        val result = rule.blockPlace(
            PlacementState(
                Mockito.mock(Instance::class.java),
                Block.OAK_LOG,
                blockFace,
                Pos(0.0, 0.0, 0.0),
                Pos(0.0, 0.0, 0.0),
                Pos(0.0, 0.0, 0.0),
                null,
                false
            )
        )!!

        Assertions.assertNotNull(result)
        Assertions.assertEquals(expectedAxis, result.getProperty("axis"))
    }

    @Test
    fun blockPlace() {
        assetBlockPlacement(BlockFace.EAST, "x")
        assetBlockPlacement(BlockFace.WEST, "x")
        assetBlockPlacement(BlockFace.NORTH, "z")
        assetBlockPlacement(BlockFace.SOUTH, "z")
        assetBlockPlacement(BlockFace.BOTTOM, "y")
        assetBlockPlacement(BlockFace.TOP, "y")
    }
}