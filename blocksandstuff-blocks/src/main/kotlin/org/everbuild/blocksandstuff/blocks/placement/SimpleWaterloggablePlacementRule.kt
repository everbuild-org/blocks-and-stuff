package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.isWater

class SimpleWaterloggablePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        return placementState
            .block
            .withProperty(
                "waterlogged",
                placementState
                    .instance
                    .getBlock(placementState.placePosition)
                    .isWater()
                    .toString()
            )
    }
}