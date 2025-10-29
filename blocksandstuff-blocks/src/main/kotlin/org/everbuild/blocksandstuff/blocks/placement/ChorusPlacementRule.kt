package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class ChorusPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (block.compare(Block.CHORUS_PLANT)) return null

        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        if (!blockBelow.compare(Block.END_STONE)) return null
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block? {
        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (blockBelow.compare(Block.END_STONE) || blockBelow.compare(Block.CHORUS_PLANT)) {
            return updateState.currentBlock
        }

        if (isAttachedOnSide(updateState)) {
            return updateState.currentBlock
        }

        return null
    }

    private fun isAttachedOnSide(updateState: UpdateState) = BlockFace.entries
        .filter { it.toDirection().horizontal() }
        .any { updateState.instance.getBlock(updateState.blockPosition.add(it.toDirection().vec())).compare(Block.CHORUS_PLANT) }
}