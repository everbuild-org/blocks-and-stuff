package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class HopperPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val placementFace = placementState.blockFace()
        val facing = when (placementFace) {
            BlockFace.BOTTOM -> "down"
            BlockFace.TOP -> "down"
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST -> {
                placementFace.oppositeFace.toDirection().name.lowercase()
            }
            else -> "down"
        }
        return placementState.block()
            .withProperty("facing", facing)
            .withProperty("enabled", "true")
    }
}