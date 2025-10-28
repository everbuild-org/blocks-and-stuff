package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import org.everbuild.blocksandstuff.common.utils.getNearestLookingDirection

class CalibratedSculkSensorPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val lookingAt = placementState.getNearestHorizontalLookingDirection()
        return placementState.block().withProperty("facing", lookingAt.opposite().name.lowercase())
    }
}