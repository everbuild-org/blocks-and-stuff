package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import org.everbuild.blocksandstuff.common.utils.getNearestLookingDirection

class CrafterPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val direction = placementState.getNearestLookingDirection()
        val horizontalDirection = placementState.getNearestHorizontalLookingDirection()
        return when(direction) {
            Direction.DOWN -> placementState.block
                .withProperty("orientation", "down_${horizontalDirection.name.lowercase()}")
            Direction.UP -> placementState.block
                .withProperty("orientation", "up_${horizontalDirection.opposite().name.lowercase()}")

            else -> placementState.block
                .withProperty("orientation", "${direction.name.lowercase()}_up")
        }
    }
}