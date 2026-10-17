package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.util.FallingBlock

class FallingBlockPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (canFallThrough(placementState.instance, placementState.placePosition.asBlockVec())) {
            spawnFalling(placementState.instance, placementState.block, placementState.placePosition.asBlockVec())
            return Block.AIR
        }
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (canFallThrough(updateState.instance, updateState.blockPosition.asBlockVec())) {
            spawnFalling(updateState.instance, updateState.currentBlock, updateState.blockPosition.asBlockVec())
            return Block.AIR
        }
        return updateState.currentBlock
    }

    private fun spawnFalling(
        getter: Block.Getter,
        block: Block,
        position: BlockVec,
    ) {
        val instance = getter as? Instance ?: return
        FallingBlock.spawn(block, instance, position)
    }

    private fun canFallThrough(
        instance: Block.Getter,
        position: BlockVec,
    ): Boolean {
        val below = instance.getBlock(position.sub(0.0, 1.0, 0.0))
        return below.air() || below.liquid() || below.replaceable()
    }
}
