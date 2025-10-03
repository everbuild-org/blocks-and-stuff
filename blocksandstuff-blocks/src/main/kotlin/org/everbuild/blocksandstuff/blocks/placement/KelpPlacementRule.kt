package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory.Companion.maybeDrop

class KelpPlacementRule(block: Block) : BlockPlacementRule(block) {

    override fun blockPlace(placementState: PlacementState): Block? {
        val bottomBlock = placementState.instance.getBlock(placementState.placePosition.sub(0.0, 1.0, 0.0))
        if (placementState.instance.getBlock(placementState.placePosition).compare(Block.WATER)) {
            if (!bottomBlock.isSolid &&
                !bottomBlock.compare(Block.KELP_PLANT) &&
                !bottomBlock.compare(Block.KELP)
            ) {
                return placementState.instance.getBlock(placementState.placePosition)
            }
            return Block.KELP
        }
        return placementState.instance.getBlock(placementState.placePosition)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.sub(0.0, 1.0, 0.0))
        val blockAbove = updateState.instance.getBlock(updateState.blockPosition.add(0.0, 1.0, 0.0))
        val currentBlock = updateState.instance.getBlock(updateState.blockPosition)

        if (!blockBelow.compare(Block.KELP_PLANT) && !blockBelow.isSolid) {
            maybeDrop(updateState)
            return Block.WATER
        }

        if ((blockAbove.compare(Block.KELP) || blockAbove.compare(Block.KELP_PLANT)) &&
            (currentBlock.compare(Block.KELP) || currentBlock.compare(Block.KELP_PLANT))
            ) {
            return Block.KELP_PLANT
        }

        return updateState.currentBlock
    }
}