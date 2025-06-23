package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class ShulkerPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        val facing = determineFacing(placementState)
        return placementState.block.withProperty("facing", facing.toDirection().name.lowercase())
    }
    
    private fun determineFacing(placementState: PlacementState): BlockFace {
        val blockFace = placementState.blockFace
        if (blockFace != null) {
            return blockFace
        }
        return BlockFace.NORTH
    }
    
    override fun blockUpdate(updateState: UpdateState): Block {
        return updateState.currentBlock
    }
}