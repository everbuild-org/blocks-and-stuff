package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction

class WallCoralPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val facing = BlockFace.fromDirection(
            Direction.valueOf(
                updateState.currentBlock.getProperty("facing").uppercase()
            )
        )

        if (!updateState.instance.getBlock(updateState.blockPosition.relative(facing.oppositeFace)).registry()
                .collisionShape().isFaceFull(facing)
        ) {
            return if (updateState.currentBlock.getProperty("waterlogged") == "true") Block.WATER else Block.AIR
        }

        return super.blockUpdate(updateState)
    }
}