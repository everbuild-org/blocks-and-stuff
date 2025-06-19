package org.everbuild.blocksandstuff.blocks.placement

import org.everbuild.blocksandstuff.blocks.placement.util.States.getAxis
import org.everbuild.blocksandstuff.blocks.placement.util.States.getFacing
import org.everbuild.blocksandstuff.blocks.placement.util.States.rotateYClockwise
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.kyori.adventure.key.Key
import org.everbuild.blocksandstuff.blocks.placement.common.AbstractConnectingBlockPlacementRule

class FencePlacementRule(block: Block) : AbstractConnectingBlockPlacementRule(block) {
    private val fences = tagManager.getTag(Key.key("minecraft:fences"))!!
    private val woodenFences = tagManager.getTag(Key.key("minecraft:wooden_fences"))!!
    private val fenceGates = tagManager.getTag(Key.key("minecraft:fence_gates"))!!

    override fun canConnect(instance: Block.Getter, pos: Point, blockFace: BlockFace): Boolean {
        val instanceBlock = instance.getBlock(pos)
        val isBlockNetherBrickFence: Boolean = block.name().endsWith("_brick_fence")
        val isInstanceBlockNetherBrickFence = instanceBlock.name().endsWith("_brick_fence")
        val canConnectToFence = canConnectToFence(instanceBlock)
        val canFenceGateConnect =
            fenceGates.contains(instanceBlock) && getAxis(getFacing(instanceBlock).toDirection()) == getAxis(
                rotateYClockwise(blockFace.toDirection())
            )
        val isFaceFull = instanceBlock.registry().collisionShape().isFaceFull(blockFace)

        return !cannotConnect.contains(block) && isFaceFull || (canConnectToFence && !isBlockNetherBrickFence) || canFenceGateConnect || (isBlockNetherBrickFence && isInstanceBlockNetherBrickFence)
    }

    private fun canConnectToFence(block: Block): Boolean {
        val isFence = fences.contains(block)
        val isWoodenFence = woodenFences.contains(block)
        return isFence && isWoodenFence
    }
}