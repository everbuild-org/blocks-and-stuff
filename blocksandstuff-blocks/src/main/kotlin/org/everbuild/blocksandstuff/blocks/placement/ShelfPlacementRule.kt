package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.isWater

class ShelfPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        var result = placementState.block
        val playerPosition = placementState.playerPosition()
        if (playerPosition != null) {
            val facing = BlockFace.fromYaw(playerPosition.yaw).oppositeFace
            result = result.withProperty("facing", facing.name.lowercase())
        }
        if (result.getProperty("waterlogged") != null) {
            result = result.withProperty("waterlogged", placementState.instance.getBlock(placementState.placePosition).isWater().toString())
        }
        return result
    }
}
