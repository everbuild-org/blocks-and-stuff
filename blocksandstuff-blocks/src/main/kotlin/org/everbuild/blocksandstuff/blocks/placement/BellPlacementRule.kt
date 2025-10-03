package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class BellPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockFace = placementState.blockFace ?: return block

        return when (blockFace) {
            BlockFace.BOTTOM -> block
                .withProperty("attachment", "ceiling")

            BlockFace.TOP -> block
                .withProperty("attachment", "floor")
                .withProperty("facing", placementState.getNearestHorizontalLookingDirection().name.lowercase())

            else -> {
                val direction = blockFace.oppositeFace.name.lowercase()
                val doubleWall = placementState.instance.getBlock(
                    placementState.placePosition.add(
                        blockFace.toDirection().vec()
                    )
                ).isSolid

                block
                    .withProperty("facing", direction)
                    .withProperty("attachment", if (doubleWall) "double_wall" else "single_wall")
            }
        }
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val attachment = updateState.currentBlock.getProperty("attachment")
        if (attachment == "ceiling" && !updateState.instance.getBlock(
                updateState.blockPosition.add(
                    0.0,
                    1.0,
                    0.0
                )
            ).isSolid
        ) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        if (attachment == "floor" && !updateState.instance.getBlock(
                updateState.blockPosition.add(
                    0.0,
                    -11.0,
                    0.0
                )
            ).isSolid
        ) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        val attachmentDirection = BlockFace.valueOf(updateState.currentBlock.getProperty("facing")!!.uppercase())
        val blockInFront = updateState.instance.getBlock(updateState.blockPosition.add(attachmentDirection.toDirection().vec()))
        val blockBehind = updateState.instance.getBlock(updateState.blockPosition.add(attachmentDirection.oppositeFace.toDirection().vec()))

        if (blockInFront.isSolid && blockBehind.isSolid) {
            return updateState.currentBlock
                .withProperty("attachment", "double_wall")
        } else if (blockInFront.isSolid) {
            return updateState.currentBlock
                .withProperty("attachment", "single_wall")
                .withProperty("facing", attachmentDirection.name.lowercase())
        } else if (blockBehind.isSolid) {
            return updateState.currentBlock
                .withProperty("attachment", "single_wall")
                .withProperty("facing", attachmentDirection.oppositeFace.name.lowercase())
        } else {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
    }
}