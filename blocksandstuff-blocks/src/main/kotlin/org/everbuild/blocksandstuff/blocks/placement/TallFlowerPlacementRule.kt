package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class TallFlowerPlacementRule (baseFlowerBlock: Block) : BlockPlacementRule(baseFlowerBlock) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val instance = placementState.instance
        val placePos = placementState.placePosition

        val upperPos = placePos.add(0.0, 1.0, 0.0)
        if (!instance.getBlock(upperPos).registry()!!.isReplaceable) {
            return null
        }

        val lowerPos = placePos.sub(0.0, 1.0, 0.0)
        if (!instance.getBlock(lowerPos).registry()!!.collisionShape().isFaceFull(BlockFace.TOP)) {
            return null
        }

        val lowerFlowerBlock = placementState.block
            .withProperty("half", "lower")

        val upperFlowerBlock = placementState.block
            .withProperty("half", "upper")

        (instance as Instance).setBlock(upperPos, upperFlowerBlock)

        return lowerFlowerBlock
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val instance = updateState.instance
        val currentBlock = updateState.currentBlock
        val blockPosition = updateState.blockPosition

        val half = currentBlock.getProperty("half")

        val neighborPos: Point
        val expectedOtherHalf: String

        if (half == "lower") {
            neighborPos = blockPosition.add(0.0, 1.0, 0.0)
            expectedOtherHalf = "upper"
        } else { // half == "upper"
            neighborPos = blockPosition.sub(0.0, 1.0, 0.0)
            expectedOtherHalf = "lower"
        }

        val neighborBlock = instance.getBlock(neighborPos)

        if (!neighborBlock.compare(updateState.currentBlock) || neighborBlock.getProperty("half") != expectedOtherHalf) {
            if (neighborBlock.compare(updateState.currentBlock)) {
                (instance as Instance).setBlock(neighborPos, Block.AIR)
            }
            return Block.AIR
        }

        val blockBelow = instance.getBlock(blockPosition.relative(BlockFace.BOTTOM))
        if (updateState.currentBlock.getProperty("half") == "lower" && !blockBelow.registry()!!.collisionShape()
                .isFaceFull(BlockFace.TOP)
        ) {
            DroppedItemFactory.maybeDrop(updateState)
            (instance as Instance).setBlock(neighborPos, Block.AIR)
            instance.setBlock(updateState.blockPosition, Block.AIR)
        }

        return updateState.currentBlock
    }
}