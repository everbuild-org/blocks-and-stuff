package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.isWater

class GroundedPlantBlockPlacementRule(
    block: Block,
) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val currentBlock = placementState.instance.getBlock(placementState.placePosition)
        val waterlogged = currentBlock.isWater().toString()
        val finalBlock =
            if (placementState.block.properties().containsKey("waterlogged")) {
                placementState.block.withProperty("waterlogged", waterlogged)
            } else {
                placementState.block
            }

        return if (isSupported(placementState.instance, placementState.placePosition)) {
            finalBlock
        } else {
            null
        }
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (!isSupported(updateState.instance, updateState.blockPosition)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }

    private fun isSupported(
        instance: Block.Getter,
        pos: Point,
    ): Boolean {
        val blockBelow = instance.getBlock(pos.sub(0.0, 1.0, 0.0))
        return blockBelow.compare(Block.DIRT) ||
            blockBelow.compare(Block.GRASS_BLOCK) ||
            blockBelow.compare(Block.COARSE_DIRT) ||
            blockBelow.compare(Block.PODZOL) ||
            blockBelow.compare(Block.ROOTED_DIRT) ||
            blockBelow.compare(Block.MOSS_BLOCK) ||
            blockBelow.compare(Block.FARMLAND) ||
            blockBelow.compare(Block.MUD) ||
            blockBelow.compare(Block.MYCELIUM)
    }
}
