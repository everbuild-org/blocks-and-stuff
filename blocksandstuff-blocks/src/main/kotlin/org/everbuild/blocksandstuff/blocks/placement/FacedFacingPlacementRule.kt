package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class FacedFacingPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockFace = placementState.blockFace() ?: return null

        val face = if (blockFace == BlockFace.TOP) "floor" else if (blockFace == BlockFace.BOTTOM) "ceiling" else "wall"
        val facing = if (blockFace != BlockFace.TOP && blockFace != BlockFace.BOTTOM)
            blockFace
        else
            BlockFace.fromDirection(placementState.getNearestHorizontalLookingDirection().opposite())

        val supporting = getSupportingBlockPosition(face, facing, placementState.placePosition)
        if (!placementState.instance.getBlock(supporting).isSolid) {
            return null
        }

        return block
            .withProperty("facing", facing.name.lowercase())
            .withProperty("face", face)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val face = updateState.currentBlock.getProperty("face")
        val facing = BlockFace.valueOf(updateState.currentBlock.getProperty("facing").uppercase())
        val supportingBlockPos = getSupportingBlockPosition(face, facing, updateState.blockPosition)

        if (!updateState.instance.getBlock(supportingBlockPos).isSolid) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return updateState.currentBlock
    }

    fun getSupportingBlockPosition(face: String, facing: BlockFace, blockPosition: Point): Point {
        return when (face) {
            "ceiling" -> blockPosition.add(0.0, 1.0, 0.0)
            "floor" -> blockPosition.sub(0.0, 1.0, 0.0)
            else -> blockPosition.add(facing.oppositeFace.toDirection().vec())
        }
    }
}