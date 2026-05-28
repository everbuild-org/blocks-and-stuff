package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getHorizontalPlacementDirection

class VerticallyRotatedPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        val playerDirection = placementState.getHorizontalPlacementDirection()
            ?: return placementState.block
        val blockFacing = playerDirection.opposite()

        return placementState.block
            .withProperty(
                "facing",
                blockFacing.name.lowercase()
            )
    }
}