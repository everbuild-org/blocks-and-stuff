package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class GlazedTerracottaPlacementRule(block: Block) : BlockPlacementRule(block) {

    override fun blockPlace(placementState: PlacementState): Block? {
        val horizontalLookingDirection = placementState.getNearestHorizontalLookingDirection()

        return placementState.block
            .withProperty("facing", horizontalLookingDirection.name.lowercase())
    }
}