package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class TopAttachedVinePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (!validatePosition(placementState.instance, placementState.placePosition)) {
            return null
        }

        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (!validatePosition(updateState.instance, updateState.blockPosition)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return updateState.currentBlock
    }

    fun validatePosition(instance: Block.Getter, position: Point): Boolean {
        val above = instance.getBlock(position.add(0.0, 1.0, 0.0))

        if (above.registry().collisionShape().isFaceFull(BlockFace.BOTTOM)) return true
        if (above.compare(block)) return true
        if (above.key().value().substring(0, 3) == block.key().value().substring(0, 3)) return true
        return false
    }
}