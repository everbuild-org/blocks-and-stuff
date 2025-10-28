package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class SeaPicklePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        val currentBlock = placementState.instance.getBlock(placementState.placePosition)
        var waterlogged: String? = "false"

        if (!blockBelow.isSolid) return null
        if (currentBlock.compare(Block.WATER) || (currentBlock.compare(Block.SEA_PICKLE)) && currentBlock.getProperty("waterlogged")!!.equals("true", ignoreCase = true)) {
            waterlogged = "true"
        }
        val current = placementState.instance.getBlock(placementState.placePosition)
        val amount =
            ((if (current.compare(block, Block.Comparator.ID)) current.getProperty("pickles")!!.toIntOrNull() ?: 0
            else 0) + 1).coerceAtMost(4)
        return placementState.block.withProperty("pickles", amount.toString()).withProperty("waterlogged", waterlogged)
    }

    override fun isSelfReplaceable(replacement: Replacement): Boolean {
        val amount =
            if (replacement.block.compare(block, Block.Comparator.ID)) replacement.block.getProperty("pickles")!!
                .toIntOrNull() ?: 0
            else 0
        return amount < 4
    }

    override fun blockUpdate(updateState: UpdateState): Block? {
        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (!blockBelow.isSolid) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }
}