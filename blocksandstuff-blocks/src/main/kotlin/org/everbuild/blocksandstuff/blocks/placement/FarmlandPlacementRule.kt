package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class FarmlandPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockUpdate(updateState: UpdateState): Block {
        val abovePosition = updateState.blockPosition.relative(BlockFace.TOP)
        val aboveBlock = updateState.instance.getBlock(abovePosition)
        if (aboveBlock.isSolid) {
            return Block.DIRT
        }
        return updateState.currentBlock
    }

    override fun blockPlace(placementState: PlacementState): Block? {
        return placementState.block
    }
}