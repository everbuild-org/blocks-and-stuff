package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory.Companion.maybeDrop

class TallSeagrassPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val belowBlock = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (!belowBlock.isSolid && !belowBlock.compare(Block.SEAGRASS)) {
            maybeDrop(updateState)
            return Block.WATER
        }
        return updateState.currentBlock
    }
}