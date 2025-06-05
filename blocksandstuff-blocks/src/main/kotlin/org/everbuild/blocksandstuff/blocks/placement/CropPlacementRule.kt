package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class CropPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        if (!blockBelow.compare(Block.FARMLAND)) return null
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (!blockBelow.compare(Block.FARMLAND)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }
}