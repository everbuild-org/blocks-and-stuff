package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler

class TwistingVinePlacementRule(block: Block) : BlockPlacementRule(block) {
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

        val above = updateState.instance.getBlock(updateState.blockPosition.add(0.0, 1.0, 0.0))

        if (updateState.currentBlock.compare(Block.TWISTING_VINES_PLANT, Block.Comparator.ID) && (!above.compare(Block.TWISTING_VINES, Block.Comparator.ID) && !above.compare(Block.TWISTING_VINES_PLANT, Block.Comparator.ID))) {
            return Block.TWISTING_VINES.withDefaultHandler()
        } else if (updateState.currentBlock.compare(Block.TWISTING_VINES, Block.Comparator.ID) && (above.compare(Block.TWISTING_VINES_PLANT, Block.Comparator.ID) || above.compare(Block.TWISTING_VINES, Block.Comparator.ID))) {
            return Block.TWISTING_VINES_PLANT.withDefaultHandler()
        }

        return updateState.currentBlock
    }

    fun validatePosition(instance: Block.Getter, position: Point): Boolean {
        val below = instance.getBlock(position.sub(0.0, 1.0, 0.0))

        if (below.registry()!!.collisionShape().isFaceFull(BlockFace.TOP)) return true
        if (below.compare(Block.TWISTING_VINES, Block.Comparator.ID) || below.compare(
                Block.TWISTING_VINES_PLANT,
                Block.Comparator.ID
            )
        ) return true
        if (below.key().value().substring(0, 3) == block.key().value().substring(0, 3)) return true
        return false
    }
}