package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getNearestLookingDirection
import org.everbuild.blocksandstuff.common.utils.isWater

class ShelfPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        var result = placementState.block
        if (placementState.playerPosition() != null) {
            result = result.withProperty("facing", placementState.getNearestLookingDirection().name.lowercase())
        }
        if (result.getProperty("waterlogged") != null) {
            result = result.withProperty("waterlogged", placementState.instance.getBlock(placementState.placePosition).isWater().toString())
        }
        return result
    }
}
