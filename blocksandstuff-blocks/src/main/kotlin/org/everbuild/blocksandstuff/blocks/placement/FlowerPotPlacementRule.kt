package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.behavior.CustomPlants
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class FlowerPotPlacementRule(
    block: Block,
) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (!isSupported(placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0)))) {
            return null
        }
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val below = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (isSupported(below)) return updateState.currentBlock

        if (updateState.currentBlock.handler() == null) {
            CustomPlants
                .findForBlock(updateState.currentBlock)
                ?.drop
                ?.takeUnless { it.isAir }
                ?.let { DroppedItemFactory.maybeDrop(updateState.instance as Instance, updateState.blockPosition, it) }
            DroppedItemFactory.maybeDrop(updateState.instance as Instance, updateState.blockPosition, Block.FLOWER_POT)
        }
        return Block.AIR
    }

    private fun isSupported(block: Block): Boolean = block.collisionShape().isFaceFull(BlockFace.TOP)
}
