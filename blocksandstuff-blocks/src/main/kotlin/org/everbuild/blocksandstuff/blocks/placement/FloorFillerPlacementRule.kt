package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class FloorFillerPlacementRule(block: Block) : BlockPlacementRule(block) {
    fun getPropertyName(): String {
        if (block == Block.LEAF_LITTER) return "segment_amount"
        return "flower_amount"
    }

    override fun blockPlace(placementState: PlacementState): Block? {
        if (!isSupported(placementState.instance, placementState.placePosition)) return null
        val facing = placementState.getNearestHorizontalLookingDirection().toString().lowercase()
        val previousBlock = placementState.instance.getBlock(placementState.placePosition)
        val isSelf = previousBlock.compare(block, Block.Comparator.ID)
        val petals = previousBlock.getProperty(getPropertyName())?.toIntOrNull() ?: 0
        return if (isSelf) {
            previousBlock
        } else {
            placementState.block.withProperty("facing", facing)
        }.withProperty(getPropertyName(), (petals + 1).toString())
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (!isSupported(updateState.instance, updateState.blockPosition)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return super.blockUpdate(updateState)
    }

    override fun isSelfReplaceable(replacement: Replacement): Boolean {
        val petals = replacement.block.getProperty(getPropertyName())?.toIntOrNull() ?: 0
        return petals < 4
    }

    fun isSupported(instance: Block.Getter, block: Point): Boolean {
        val below = instance.getBlock(block.sub(0.0, 1.0, 0.0))
        return below.registry()!!.collisionShape().isFaceFull(BlockFace.TOP)
    }
}