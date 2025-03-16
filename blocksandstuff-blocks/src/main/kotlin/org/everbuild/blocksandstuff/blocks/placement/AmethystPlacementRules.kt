package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.isWater

class AmethystPlacementRules(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val anchor = placementState.placePosition.sub((placementState.blockFace ?: return null).toDirection().vec())
        val anchorBlock = placementState.instance.getBlock(anchor)
        if (!anchorBlock.registry().collisionShape().isFaceFull(placementState.blockFace!!)) return null
        val currentBlock = placementState.instance.getBlock(placementState.placePosition)
        val waterlogged = currentBlock.isWater()

        return placementState.block
            .withProperty("waterlogged", waterlogged.toString())
            .withProperty("facing", placementState.blockFace!!.toDirection().name.lowercase())
    }
}