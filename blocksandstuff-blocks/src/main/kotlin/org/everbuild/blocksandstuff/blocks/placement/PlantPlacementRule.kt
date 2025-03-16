package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class PlantPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        if (!blockBelow.compare(Block.FARMLAND)) return null
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (!blockBelow.compare(Block.FARMLAND)) {
            if (DROP_ITEMS) {
                DroppedItemFactory.current.spawn(
                    updateState.instance as Instance,
                    updateState.blockPosition,
                    updateState.currentBlock
                )
            }
            return Block.AIR
        }
        return updateState.currentBlock
    }

    companion object {
        var DROP_ITEMS = true
    }
}