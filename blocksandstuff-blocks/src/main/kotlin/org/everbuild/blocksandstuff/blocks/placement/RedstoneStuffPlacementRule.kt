package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import java.util.*

class RedstoneStuffPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val supportPosition = placementState.placePosition.relative(BlockFace.BOTTOM)
        val supportBlock = placementState.instance.getBlock(supportPosition)
        if (supportBlock.isAir || supportBlock.compare(block)) {
            return null
        }
        val facing = placementState.getNearestHorizontalLookingDirection()
        return block
            .withProperty("facing", facing.name.lowercase(Locale.ROOT))
            .withProperty("powered", "false")
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val supportPosition = updateState.blockPosition.relative(BlockFace.BOTTOM)
        val supportBlock = updateState.instance.getBlock(supportPosition)

        if (supportBlock.isAir) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }
}