package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.TagKey
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class GroundedPlantBlockPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val dirtBlocks = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:dirt"))!!

    override fun blockPlace(placementState: PlacementState): Block? {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))

        if (dirtBlocks.contains(blockBelow)) {
            return placementState.block
        }
        return null

    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))

        if (!dirtBlocks.contains(blockBelow)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return updateState.currentBlock

    }
}