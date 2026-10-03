package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class ShelfMushroomPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val preferred = placementState.getNearestHorizontalLookingDirection()
        val validDirections = Direction.HORIZONTAL
        val checkedOrder = listOf(preferred, *validDirections)

        for (direction in checkedOrder) {
            if (!canSupport(
                    placementState.instance,
                    placementState.placePosition.relative(BlockFace.fromDirection(direction.opposite())),
                    BlockFace.fromDirection(direction)
                )
            ) continue
            return block.withProperty("facing", direction.name.lowercase())
        }
        return null
    }

    override fun blockUpdate(updateState: UpdateState): Block? {
        val facing = updateState.currentBlock.getProperty("facing")?.let { BlockFace.valueOf(it.uppercase()) } ?: return Block.AIR
        if (!canSupport(updateState.instance, updateState.blockPosition.relative(facing.oppositeFace), facing)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }

    fun canSupport(instance: Block.Getter, blockPosition: Point, side: BlockFace): Boolean =
        instance.getBlock(blockPosition).collisionShape().isFaceFull(side)
}