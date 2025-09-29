package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import org.everbuild.blocksandstuff.common.utils.isWater

class CampfireBlockPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        val facing = BlockFace.fromDirection(placementState.getNearestHorizontalLookingDirection().opposite())
        val waterlogged = placementState.instance.getBlock(placementState.placePosition).isWater()
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        val signalFire = blockBelow.compare(Block.HAY_BLOCK)
        return placementState.block
            .withProperty("facing", facing.name.lowercase())
            .withProperty("waterlogged", waterlogged.toString())
            .withProperty("lit", (!waterlogged).toString())
            .withProperty("signal_fire", signalFire.toString())
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        val signalFire = blockBelow.compare(Block.HAY_BLOCK)
        return updateState.currentBlock
            .withProperty("signal_fire", signalFire.toString())
    }
}