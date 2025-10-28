package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class AttachedStemPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockUpdate(updateState: UpdateState): Block {
        if (updateState.instance.getBlock(updateState.blockPosition().add(0.0, -1.0, 0.0)) != Block.FARMLAND) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        val connectedSite = updateState.currentBlock().getProperty("facing")
        return updateState.currentBlock
    }

    override fun blockPlace(placementState: PlacementState): Block {
        return placementState.block
    }
}