package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory.Companion.maybeDrop
import kotlin.random.Random

class KelpPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        val bottomBlock = placementState.instance.getBlock(placementState.placePosition.relative(BlockFace.BOTTOM))
        if (placementState.instance.getBlock(placementState.placePosition).compare(Block.WATER)) {
            if (!bottomBlock.solid() &&
                !bottomBlock.compare(Block.KELP_PLANT) &&
                !bottomBlock.compare(Block.KELP)
            ) {
                return placementState.instance.getBlock(placementState.placePosition)
            }
            return Block.KELP
        }
        return placementState.instance.getBlock(placementState.placePosition)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.relative(BlockFace.BOTTOM))
        val blockAbove = updateState.instance.getBlock(updateState.blockPosition.relative(BlockFace.TOP))
        val currentBlock = updateState.currentBlock

        if (!blockBelow.compare(Block.KELP_PLANT) && !blockBelow.compare(Block.KELP) && !blockBelow.solid()) {
            maybeDrop(updateState)
            return Block.WATER
        }

        val hasKelpAbove = blockAbove.compare(Block.KELP) || blockAbove.compare(Block.KELP_PLANT)
        if (hasKelpAbove && (currentBlock.compare(Block.KELP) || currentBlock.compare(Block.KELP_PLANT))) {
            return Block.KELP_PLANT
        }

        if (currentBlock.compare(Block.KELP_PLANT)) {
            return Block.KELP.withProperty("age", Random.nextInt(25).toString())
        }

        return currentBlock
    }
}
