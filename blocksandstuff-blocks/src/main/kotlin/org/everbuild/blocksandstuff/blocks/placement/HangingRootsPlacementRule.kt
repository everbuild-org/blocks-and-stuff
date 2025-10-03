package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class HangingRootsPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockAbove = placementState.instance.getBlock(placementState.placePosition.add(0.0, 1.0, 0.0))
        if (!blockAbove.isSolid) return null
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block? {
        val blockAbove = updateState.instance.getBlock(updateState.blockPosition.add(0.0, +1.0, 0.0))
        if (!blockAbove.isSolid) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }
}