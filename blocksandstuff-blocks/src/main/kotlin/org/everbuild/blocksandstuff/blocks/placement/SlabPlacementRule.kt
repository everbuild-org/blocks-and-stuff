package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class SlabPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        val replacingBlock = placementState.instance().getBlock(placementState.placePosition())
        if (replacingBlock.compare(block)) {
            return placementState.block().withProperty("type", "double").withProperty("waterlogged", "false")
        }

        val waterlogged = replacingBlock.compare(Block.WATER).toString()

        if (placementState.blockFace() == BlockFace.BOTTOM || (placementState.blockFace() != BlockFace.TOP && (if (placementState.cursorPosition() != null) placementState.cursorPosition()!!
                .y() else 0.0) > 0.5)
        ) {
            return placementState.block().withProperty("type", "top").withProperty("waterlogged", waterlogged)
        }
        return placementState.block().withProperty("type", "bottom").withProperty("waterlogged", waterlogged)
    }

    override fun isSelfReplaceable(replacement: Replacement): Boolean {
        val blockToPlace = replacement.material().block()
        val placedBlock = replacement.block()
        if (!blockToPlace.compare(placedBlock)) return false
        val type = placedBlock.getProperty("type") ?: return false
        if (type == "double") return false

        if (type == "top" && replacement.blockFace() == BlockFace.BOTTOM) return true
        if (type == "bottom" && replacement.blockFace() == BlockFace.TOP) return true

        if (type == "top" && replacement.cursorPosition().y() < 0.5) return true
        if (type == "bottom" && replacement.cursorPosition().y() > 0.5) return true
        return false
    }
}
