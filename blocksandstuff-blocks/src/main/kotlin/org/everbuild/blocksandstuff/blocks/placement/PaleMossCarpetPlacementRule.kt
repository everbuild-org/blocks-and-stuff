package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class PaleMossCarpetPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0)).isAir) return null
        val collection = mutableListOf<BlockFace>()
        for (direction in Direction.HORIZONTAL) {
            if (placementState.instance.getBlock(placementState.placePosition.relative(BlockFace.fromDirection(direction)))
                    .registry()!!.collisionShape().isFaceFull(BlockFace.fromDirection(direction).oppositeFace)
            ) {
                collection.add(BlockFace.fromDirection(direction))
            }
        }
        return block.withProperties(
            mapOf(
                "east" to if (collection.contains(BlockFace.EAST)) "low" else "none",
                "west" to if (collection.contains(BlockFace.WEST)) "low" else "none",
                "north" to if (collection.contains(BlockFace.NORTH)) "low" else "none",
                "south" to if (collection.contains(BlockFace.SOUTH)) "low" else "none"
            )
        )
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val collection = mutableListOf<BlockFace>()
        if (updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0)).isAir) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        for (direction in Direction.HORIZONTAL) {
            if (updateState.instance.getBlock(updateState.blockPosition.relative(BlockFace.fromDirection(direction)))
                    .registry()!!.collisionShape().isFaceFull(BlockFace.fromDirection(direction).oppositeFace)
            ) {
                collection.add(BlockFace.fromDirection(direction))
            }
        }
        return block.withProperties(
            mapOf(
                "east" to if (collection.contains(BlockFace.EAST)) "low" else "none",
                "west" to if (collection.contains(BlockFace.WEST)) "low" else "none",
                "north" to if (collection.contains(BlockFace.NORTH)) "low" else "none",
                "south" to if (collection.contains(BlockFace.SOUTH)) "low" else "none"
            )
        )
    }
}