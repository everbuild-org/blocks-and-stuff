package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.isWater

open class LightningRodPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockFace = placementState.blockFace() ?: return null
        val facing = blockFace/*if (blockFace != BlockFace.TOP && blockFace != BlockFace.BOTTOM)
            blockFace
        else
            BlockFace.fromDirection(placementState.getNearestHorizontalLookingDirection().opposite())**/

        val supporting = getSupportingBlockPosition(facing, placementState.placePosition)
        if (needSupport() && !placementState.instance.getBlock(supporting).isSolid) {
            return null
        }

        return block
            .withProperty("facing",
                when (facing) {
                    BlockFace.BOTTOM -> "down"
                    BlockFace.TOP -> "up"
                    else -> blockFace.name.lowercase()
                }
            )
            .withProperty(
                "waterlogged",
                placementState
                    .instance
                    .getBlock(placementState.placePosition)
                    .isWater()
                    .toString()
            )
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val facing = if (updateState.currentBlock.getProperty("facing").equals("up")) BlockFace.TOP else if (updateState.currentBlock.getProperty("facing").equals("down")) BlockFace.BOTTOM else BlockFace.valueOf(updateState.currentBlock.getProperty("facing").uppercase())
        val supportingBlockPos = getSupportingBlockPosition(facing, updateState.blockPosition)

        if (needSupport() && (updateState.instance.getBlock(supportingBlockPos).isLiquid || updateState.instance.getBlock(supportingBlockPos).isAir)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return updateState.currentBlock
    }

    fun getSupportingBlockPosition(facing: BlockFace, blockPosition: Point): Point {
        return when (facing) {
            BlockFace.BOTTOM -> blockPosition.add(0.0, 1.0, 0.0)
            BlockFace.TOP -> blockPosition.sub(0.0, 1.0, 0.0)
            else -> blockPosition.add(facing.oppositeFace.toDirection().vec())
        }
    }

    open fun needSupport(): Boolean {
        return true
    }
}