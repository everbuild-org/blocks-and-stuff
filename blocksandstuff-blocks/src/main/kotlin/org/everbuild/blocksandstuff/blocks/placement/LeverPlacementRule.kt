package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.canAttach
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import java.util.*

class LeverPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val clickedFace = placementState.blockFace ?: return null
        if (!placementState.canAttach()) {
            return null
        }
        val newBlock = block.withProperty("powered", "false")
        return when (clickedFace) {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST -> {
                newBlock
                    .withProperty("face", "wall")
                    .withProperty("facing", clickedFace.name.lowercase(Locale.ROOT))
            }

            BlockFace.TOP -> {
                val playerFacing = placementState.getNearestHorizontalLookingDirection().opposite()
                newBlock
                    .withProperty("face", "floor")
                    .withProperty("facing", playerFacing.name.lowercase(Locale.ROOT))
            }

            BlockFace.BOTTOM -> {
                val playerFacing = placementState.getNearestHorizontalLookingDirection().opposite()
                newBlock
                    .withProperty("face", "ceiling")
                    .withProperty("facing", playerFacing.name.lowercase(Locale.ROOT))
            }
        }
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val currentBlock = updateState.currentBlock
        val face = currentBlock.getProperty("face") ?: return Block.AIR
        val facing = currentBlock.getProperty("facing") ?: return Block.AIR
        val supportDirection = when (face) {
            "floor" -> BlockFace.BOTTOM
            "ceiling" -> BlockFace.TOP
            "wall" -> BlockFace.valueOf(facing.uppercase(Locale.ROOT)).oppositeFace // Support is behind the lever.
            else -> null
        }

        if (supportDirection == null) {
            return Block.AIR
        }

        val supportBlockPosition = updateState.blockPosition.relative(supportDirection)
        val supportBlock = updateState.instance.getBlock(supportBlockPosition)
        val attachedFace = supportDirection.oppositeFace

        if (!supportBlock.registry().collisionShape().isFaceFull(attachedFace)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }
}