package org.everbuild.blocksandstuff.blocks.placement.common

import java.util.Map
import kotlin.collections.plus
import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.RegistryTag
import org.everbuild.blocksandstuff.blocks.placement.util.States

abstract class AbstractConnectingBlockPlacementRule(block: Block) : BlockPlacementRule(block) {
    protected val tagManager = Block.staticRegistry()
    protected val leaves = tagManager.getTag(Key.key("minecraft:leaves"))!!
    protected val shulkerBoxes = tagManager.getTag(Key.key("minecraft:shulker_boxes"))!!
    protected val cannotConnect = RegistryTag.direct(
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
            java.util.Map.of<String, String>(
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

    abstract fun canConnect(instance: Block.Getter, pos: Point, blockFace: BlockFace): Boolean
}