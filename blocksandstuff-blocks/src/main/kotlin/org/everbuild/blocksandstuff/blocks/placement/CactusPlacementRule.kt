package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.tag.BlockTags

class CactusPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val plantableOn = BlockTags.getTaggedWith("minecraft:sand")

    override fun blockPlace(placementState: PlacementState): Block? {
        if (checkEligibility(placementState.instance, placementState.placePosition)) return placementState.block
        return null
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (checkEligibility(updateState.instance, updateState.blockPosition)) return updateState.currentBlock
        DroppedItemFactory.maybeDrop(updateState)
        return Block.AIR
    }

    fun checkEligibility(instance: Block.Getter, position: Point): Boolean {
        val blockBelow = instance.getBlock(position.sub(0.0, 1.0, 0.0))
        if (plantableOn.none { it.compare(blockBelow) } && !blockBelow.compare(Block.CACTUS)) return false

        for (direction in Direction.HORIZONTAL) {
            if (!instance.getBlock(position.add(direction.vec())).isAir) return false
        }

        return true
    }
}