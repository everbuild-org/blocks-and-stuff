package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class JigsawPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val nearestLooking = placementState.getNearestHorizontalLookingDirection().toString().lowercase()
        val face = placementState.blockFace ?: return placementState.block
        val state = when(face) {
            BlockFace.TOP -> "up_$nearestLooking"
            BlockFace.BOTTOM -> "down_$nearestLooking"
            else -> "${face.toString().lowercase()}_up"
        }

        return block.withProperty("orientation", state)
    }
}