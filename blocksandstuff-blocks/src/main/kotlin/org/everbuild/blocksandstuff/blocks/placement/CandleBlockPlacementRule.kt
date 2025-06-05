package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import org.everbuild.blocksandstuff.common.utils.isWater

class CandleBlockPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        if (!blockBelow.isSolid) return null
        val oldBlock = placementState.instance.getBlock(placementState.placePosition)
        val waterlogged = oldBlock.isWater() || oldBlock.getProperty("waterlogged")?.toBoolean() ?: false
        val oldCandles = oldBlock.getProperty("candles")?.toIntOrNull() ?: 0
        return placementState.block
            .withProperty("waterlogged", waterlogged.toString())
            .withProperty("lit", (!waterlogged).toString())
            .withProperty("candles", (oldCandles + 1).toString())
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (blockBelow.isSolid) return updateState.currentBlock
        DroppedItemFactory.maybeDrop(updateState)
        return Block.AIR
    }

    override fun isSelfReplaceable(replacement: Replacement): Boolean {
        val candles = replacement.block.getProperty("candles")?.toIntOrNull() ?: return false
        return candles < 4
    }
}