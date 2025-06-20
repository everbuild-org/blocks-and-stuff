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
        return transmute(instance, placePos, getProperty(updateState.currentBlock, instance, placePos))
    }

    override fun blockPlace(placementState: PlacementState): Block? {
        val instance = placementState.instance()
        val placePos = placementState.placePosition()
        return transmute(instance, placePos,getProperty(placementState.block, instance, placePos))
    }

    private fun getProperty(
        block: Block,
        instance: Block.Getter,
        placePos: Point
    ): Block {
        val north = placePos.relative(BlockFace.NORTH)
        val east = placePos.relative(BlockFace.EAST)
        val south = placePos.relative(BlockFace.SOUTH)
        val west = placePos.relative(BlockFace.WEST)

        return block.withProperties(
            Map.of<String, String>(
                States.NORTH, stringify(canConnect(instance, north, BlockFace.SOUTH), instance, north, BlockFace.SOUTH),
                States.EAST, stringify(canConnect(instance, east, BlockFace.WEST), instance, east, BlockFace.WEST),
                States.SOUTH, stringify(canConnect(instance, south, BlockFace.NORTH), instance, south, BlockFace.NORTH),
                States.WEST, stringify(canConnect(instance, west, BlockFace.EAST), instance, west, BlockFace.EAST)
            )
        )
    }

    abstract fun canConnect(instance: Block.Getter, pos: Point, blockFace: BlockFace): Boolean

    open fun stringify(connect: Boolean, instance: Block.Getter, pos: Point, direction: BlockFace): String =
        connect.toString()

    open fun transmute(instance: Block.Getter, pos: Point, block: Block): Block = block
}