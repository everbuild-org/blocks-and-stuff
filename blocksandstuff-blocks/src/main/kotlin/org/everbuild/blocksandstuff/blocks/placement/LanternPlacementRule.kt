package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class LanternPlacementRule(block: Block) : BlockPlacementRule(block) {

    override fun blockPlace(placementState: PlacementState): Block? {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        val blockAbove = placementState.instance.getBlock(placementState.placePosition.add(0.0, 1.0, 0.0))
        val canStandOnBlock = blockBelow.registry().collisionShape().isFaceFull(BlockFace.TOP)
        val canHangFromBlock = blockAbove.registry().collisionShape().isFaceFull(BlockFace.BOTTOM)

        return when {
            canHangFromBlock && (placementState.blockFace == BlockFace.BOTTOM) -> {
                placementState.block.withProperty("hanging", "true")
            }
            canStandOnBlock -> {
                placementState.block.withProperty("hanging", "false")
            }
            canHangFromBlock -> {
                placementState.block.withProperty("hanging", "true")
            }
            else -> null
        }
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val currentBlock = updateState.currentBlock
        val isHanging = currentBlock.getProperty("hanging")?.equals("true") ?: false

        if (isHanging) {
            val blockAbove = updateState.instance.getBlock(updateState.blockPosition.add(0.0, 1.0, 0.0))
            if (!blockAbove.registry().collisionShape().isFaceFull(BlockFace.BOTTOM)) {
                DroppedItemFactory.maybeDrop(updateState)
                return Block.AIR
            }
        } else {
            val blockBelow = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
            if (!blockBelow.registry().collisionShape().isFaceFull(BlockFace.TOP)) {
                DroppedItemFactory.maybeDrop(updateState)
                return Block.AIR
            }
        }
        return currentBlock
    }
}