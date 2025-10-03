package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class NetherWartPlacementRule(block: Block): BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (!placementState.instance.getBlock(placementState.placePosition.relative(BlockFace.BOTTOM)).compare(Block.SOUL_SAND)) return null
        return placementState.block()
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (!updateState.instance.getBlock(updateState.blockPosition.relative(BlockFace.BOTTOM)).compare(Block.SOUL_SAND)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }
}