package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.instance.block.rule.BlockPlacementRule.PlacementState
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class GlazedTerracottaPlacementRule(block: Block) : BlockPlacementRule(block) {

    override fun blockPlace(placementState: PlacementState): Block? {
        val horizontalLookingDirection = placementState.getNearestHorizontalLookingDirection()

        return block.withProperty("facing", horizontalLookingDirection.name.lowercase())
    }
}