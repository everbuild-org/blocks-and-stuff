package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class PointedDripstonePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockFace = placementState.blockFace()
        val direction = when (blockFace) {
            BlockFace.TOP -> "up"
            BlockFace.BOTTOM -> "down"
            else -> return null
        }
        val thickness = getThickness(placementState.instance(), placementState.placePosition(), direction == "up")
        return block.withProperties(mapOf(
            "vertical_direction" to direction,
            "thickness" to thickness
        ))
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val direction = updateState.currentBlock().getProperty("vertical_direction").equals("up")
        val newThickness = getThickness(updateState.instance(), updateState.blockPosition(), direction)
        val bottomSupported = updateState.instance().getBlock(updateState.blockPosition().add(0.0, -1.0, 0.0))
        val topSupported = updateState.instance().getBlock(updateState.blockPosition().add(0.0, 1.0, 0.0))
        if (direction && !bottomSupported.registry()!!.collisionShape().isFaceFull(BlockFace.TOP) && !bottomSupported.compare(Block.POINTED_DRIPSTONE)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        if (!direction && !topSupported.registry()!!.collisionShape().isFaceFull(BlockFace.BOTTOM) && !topSupported.compare(Block.POINTED_DRIPSTONE)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock().withProperty("thickness", newThickness)
    }

    private fun getThickness(instance: Block.Getter, blockPosition: Point, direction: Boolean): String {
        val abovePosition = blockPosition.relative(if (direction) BlockFace.TOP else BlockFace.BOTTOM)
        val aboveBlock = instance.getBlock(abovePosition)
        val aboveThickness = aboveBlock.getProperty("thickness")
        val belowPosition = blockPosition.add(0.0, if (direction) -1.0 else 1.0, 0.0)
        val belowBlock = instance.getBlock(belowPosition, Block.Getter.Condition.TYPE)

        if (!aboveBlock.compare(Block.POINTED_DRIPSTONE, Block.Comparator.ID)) {
            return "tip"
        }
        if (aboveBlock.getProperty("vertical_direction") == if (direction) "down" else "up") {
            return "tip_merge"
        }
        if (aboveThickness == "tip" || aboveThickness == "tip_merge") {
            return "frustum"
        }
        if (belowBlock.id() != Block.POINTED_DRIPSTONE.id())
            return "base"
        return "middle"
    }
}