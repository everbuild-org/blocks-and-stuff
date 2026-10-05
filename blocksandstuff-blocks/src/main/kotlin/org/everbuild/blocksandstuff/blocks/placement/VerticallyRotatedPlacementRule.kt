package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getHorizontalPlacementDirection

class VerticallyRotatedPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        val playerDirection = placementState.getHorizontalPlacementDirection()
            ?: return placementState.block

        return placementState.block
            .withProperty(
                "facing",
                playerDirection.opposite().name.lowercase()
            )
    }
}