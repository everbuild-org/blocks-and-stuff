package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getHorizontalPlacementDirection

class VerticallyRotatedPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        return placementState.block
            .withProperty(
                "facing",
                (placementState.getHorizontalPlacementDirection() ?: return placementState.block).name.lowercase()
            )
    }
}