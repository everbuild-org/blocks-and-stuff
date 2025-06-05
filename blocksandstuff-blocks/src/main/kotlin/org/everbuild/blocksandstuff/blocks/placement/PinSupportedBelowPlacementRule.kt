package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class PinSupportedBelowPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0)).isAir) return null
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0)).isAir) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }
}