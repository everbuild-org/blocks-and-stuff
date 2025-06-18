package org.everbuild.blocksandstuff.blocks.placement

import org.everbuild.blocksandstuff.blocks.placement.util.States
import org.everbuild.blocksandstuff.blocks.placement.util.States.getAxis
import org.everbuild.blocksandstuff.blocks.placement.util.States.getFacing
import org.everbuild.blocksandstuff.blocks.placement.util.States.rotateYClockwise
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import java.util.Map
import net.kyori.adventure.key.Key
import net.minestom.server.registry.RegistryTag

class FencePlacementRule(block: Block) : BlockPlacementRule(block) {
    private val tagManager = Block.staticRegistry()
    private val fences = tagManager.getTag(Key.key("minecraft:fences"))!!
    private val woodenFences = tagManager.getTag(Key.key("minecraft:wooden_fences"))!!
    private val fenceGates = tagManager.getTag(Key.key("minecraft:fence_gates"))!!

    private val leaves = tagManager.getTag(Key.key("minecraft:leaves"))!!
    private val shulkerBoxes = tagManager.getTag(Key.key("minecraft:shulker_boxes"))!!
    private val cannotConnect = RegistryTag.direct<Block>(
        shulkerBoxes.toList()
                + leaves.toList()
                + listOf(Block.BARRIER, Block.CARVED_PUMPKIN, Block.JACK_O_LANTERN, Block.MELON, Block.PUMPKIN)
    )

    override fun blockUpdate(updateState: UpdateState): Block {
        val instance = updateState.instance()
        val placePos = updateState.blockPosition()
        val north = placePos.relative(BlockFace.NORTH)
        val east = placePos.relative(BlockFace.EAST)
        val south = placePos.relative(BlockFace.SOUTH)
        val west = placePos.relative(BlockFace.WEST)

        return updateState.currentBlock().withProperties(
            Map.of<String, String>(
                States.NORTH, canConnect(instance, north, BlockFace.SOUTH).toString(),
                States.EAST, canConnect(instance, east, BlockFace.WEST).toString(),
                States.SOUTH, canConnect(instance, south, BlockFace.NORTH).toString(),
                States.WEST, canConnect(instance, west, BlockFace.EAST).toString()
            )
        )
    }

    override fun blockPlace(placementState: PlacementState): Block? {
        val instance = placementState.instance()
        val placePos = placementState.placePosition()
        val north = placePos.relative(BlockFace.NORTH)
        val east = placePos.relative(BlockFace.EAST)
        val south = placePos.relative(BlockFace.SOUTH)
        val west = placePos.relative(BlockFace.WEST)


        return placementState.block().withProperties(
            Map.of<String, String>(
                States.NORTH, canConnect(instance, north, BlockFace.SOUTH).toString(),
                States.EAST, canConnect(instance, east, BlockFace.WEST).toString(),
                States.SOUTH, canConnect(instance, south, BlockFace.NORTH).toString(),
                States.WEST, canConnect(instance, west, BlockFace.EAST).toString()
            )
        )
    }

    private fun canConnect(instance: Block.Getter, pos: Point, blockFace: BlockFace): Boolean {
        val instanceBlock = instance.getBlock(pos)
        val isBlockNetherBrickFence: Boolean = block.name().endsWith("_brick_fence")
        val isInstanceBlockNetherBrickFence = instanceBlock.name().endsWith("_brick_fence")
        val canConnectToFence = canConnectToFence(instanceBlock)
        val canFenceGateConnect = fenceGates.contains(instanceBlock) && getAxis(getFacing(instanceBlock).toDirection()) == getAxis(rotateYClockwise(blockFace.toDirection()))
        val isFaceFull = instanceBlock.registry().collisionShape().isFaceFull(blockFace)

        return !cannotConnect.contains(block) && isFaceFull || (canConnectToFence && !isBlockNetherBrickFence) || canFenceGateConnect || (isBlockNetherBrickFence && isInstanceBlockNetherBrickFence)
    }

    private fun canConnectToFence(block: Block): Boolean {
        val isFence = fences.contains(block)
        val isWoodenFence = woodenFences.contains(block)
        return isFence && isWoodenFence
    }
}