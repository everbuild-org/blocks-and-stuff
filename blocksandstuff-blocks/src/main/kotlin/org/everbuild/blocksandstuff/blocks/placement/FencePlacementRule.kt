package org.everbuild.blocksandstuff.blocks.placement

import org.everbuild.blocksandstuff.blocks.placement.util.States
import org.everbuild.blocksandstuff.blocks.placement.util.States.getAxis
import org.everbuild.blocksandstuff.blocks.placement.util.States.getFacing
import org.everbuild.blocksandstuff.blocks.placement.util.States.rotateYClockwise
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.gamedata.tags.Tag
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import java.util.Map

class FencePlacementRule(block: Block) : BlockPlacementRule(block) {
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
        val canFenceGateConnect =
            instanceBlock.name().endsWith("_fence_gate") && getAxis(getFacing(instanceBlock).toDirection()) == getAxis(
                rotateYClockwise(blockFace.toDirection())
            )
        val isFaceFull = instanceBlock.registry().collisionShape().isFaceFull(blockFace)


        return !cannotConnect(instanceBlock) && isFaceFull || (canConnectToFence && !isBlockNetherBrickFence) || canFenceGateConnect || (isBlockNetherBrickFence && isInstanceBlockNetherBrickFence)
    }

    private fun canConnectToFence(block: Block): Boolean {
        val tagManager = MinecraftServer.getTagManager()
        val isFence = tagManager.getTag(Tag.BasicType.BLOCKS, "minecraft:fences")!!.contains(block.key())
        val isWoodenFence =
            tagManager.getTag(Tag.BasicType.BLOCKS, "minecraft:wooden_fences")!!.contains(block.key())

        return isFence && isWoodenFence
    }

    private fun cannotConnect(block: Block): Boolean {
        val name = block.name().replace("minecraft:".toRegex(), "")
        return name.endsWith("leaves")
                ||
                name == "barrier"
                ||
                name == "carved_pumpkin"
                ||
                name == "jack_o_lantern"
                ||
                name == "melon"
                ||
                name == "pumpkin"
                ||
                name.endsWith("_shulker_box")
    }
}