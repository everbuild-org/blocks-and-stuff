package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class RedstoneWirePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val position = placementState.placePosition ?: return placementState.block
        return WireConnections.updatedWire(placementState.instance, position, placementState.block, preserveDot = false)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val updated = WireConnections.updatedWire(
            updateState.instance,
            updateState.blockPosition,
            updateState.currentBlock,
        )

        if (updated == null) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return updated
    }
}
