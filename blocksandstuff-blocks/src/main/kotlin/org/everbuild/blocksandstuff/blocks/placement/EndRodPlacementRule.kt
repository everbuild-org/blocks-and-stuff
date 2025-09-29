package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class EndRodPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockFace = placementState.blockFace() ?: return null
        val supportingPos = getSupportingBlockPosition(blockFace, placementState.placePosition)
        val supportingBlock = placementState.instance.getBlock(supportingPos)
        if (supportingBlock.compare(Block.END_ROD)) {
            val supportingFacing = supportingBlock.getProperty("facing") ?: "up"
            val supportingFace = runCatching { BlockFace.valueOf(supportingFacing.uppercase()) }.getOrNull()
            val newFacing = when (blockFace) {
                BlockFace.TOP -> if (supportingFacing == "up") "down" else "up"
                BlockFace.BOTTOM -> if (supportingFacing == "down") "up" else "down"
                else -> {
                    if (supportingFace != null && supportingFace == blockFace.oppositeFace) {
                        blockFace.toString().lowercase()
                    } else if (supportingFace != null && supportingFace == blockFace) {
                        blockFace.oppositeFace.toString().lowercase()
                    } else {
                        blockFace.toString().lowercase()
                    }
                }
            }
            return block.withProperty("facing", newFacing)
        }
        return block.withProperty(
            "facing",
            when (blockFace) {
                BlockFace.BOTTOM -> "down"
                BlockFace.TOP -> "up"
                else -> blockFace.name.lowercase()
            }
        )
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val current = updateState.currentBlock
        val pos = updateState.blockPosition
        val instance = updateState.instance
        val neighborFaces = arrayOf(
            BlockFace.TOP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST, BlockFace.BOTTOM
        )
        var shouldBreak = false
        for (face in neighborFaces) {
            val neighborPos = when (face) {
                BlockFace.TOP -> pos.add(0.0, 1.0, 0.0)
                BlockFace.BOTTOM -> pos.sub(0.0, 1.0, 0.0)
                else -> pos.add(face.toDirection().vec())
            }
            val isWater = instance.getBlock(neighborPos).compare(Block.WATER)
            if (isWater) {
                if (face == BlockFace.BOTTOM) continue
                shouldBreak = true
                break
            }
        }
        if (shouldBreak) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        val isWaterHere = instance.getBlock(pos).compare(Block.WATER)
        if (isWaterHere) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return current
    }

    fun getSupportingBlockPosition(facing: BlockFace, blockPosition: Point): Point {
        return when (facing) {
            BlockFace.BOTTOM -> blockPosition.add(0.0, 1.0, 0.0)
            BlockFace.TOP -> blockPosition.sub(0.0, 1.0, 0.0)
            else -> blockPosition.add(facing.oppositeFace.toDirection().vec())
        }
    }
}