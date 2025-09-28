package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class ChainPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        val placementFace = placementState.blockFace()
        val axis = when (placementFace) {
            BlockFace.TOP, BlockFace.BOTTOM -> "y"
            BlockFace.NORTH, BlockFace.SOUTH -> "z"
            BlockFace.EAST, BlockFace.WEST -> "x"
            else -> "y"
        }
        return placementState.block().withProperty("axis", axis)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        return updateState.currentBlock()
    }
}