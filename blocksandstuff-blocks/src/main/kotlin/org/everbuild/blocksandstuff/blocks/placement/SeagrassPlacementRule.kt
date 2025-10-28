package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory.Companion.maybeDrop

class SeagrassPlacementRule(block: Block) : BlockPlacementRule(block) {

    override fun blockPlace(placementState: PlacementState): Block {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        val currentBlock = placementState.instance.getBlock(placementState.placePosition)
        if (!blockBelow.isSolid) return currentBlock
        if (!currentBlock.compare(Block.WATER)) return currentBlock
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val belowBlock = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (!belowBlock.isSolid) {
            maybeDrop(updateState)
            return Block.WATER
        }
        return updateState.currentBlock
    }
}