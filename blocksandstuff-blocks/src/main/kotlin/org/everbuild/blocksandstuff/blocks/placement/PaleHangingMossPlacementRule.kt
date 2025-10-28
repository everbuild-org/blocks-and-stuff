package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class PaleHangingMossPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val belowBlock = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        val aboveBlock = placementState.instance.getBlock(placementState.placePosition.add(0.0, 1.0, 0.0))
        if (!aboveBlock.isSolid && !aboveBlock.compare(Block.PALE_HANGING_MOSS)) return null
        if (placementState.blockFace() != BlockFace.BOTTOM) return null
        if (belowBlock.compare(Block.PALE_HANGING_MOSS)) {
            return placementState.block.withProperty("tip", "false")
        }
        return placementState.block.withProperty("tip", "true")
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val belowBlock = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        val aboveBlock = updateState.instance.getBlock(updateState.blockPosition.add(0.0, 1.0, 0.0))
        if (aboveBlock == Block.AIR) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        if (belowBlock.compare(Block.PALE_HANGING_MOSS)) {
            return updateState.currentBlock.withProperty("tip", "false")
        }
        return updateState.currentBlock.withProperty("tip", "true")
    }
}