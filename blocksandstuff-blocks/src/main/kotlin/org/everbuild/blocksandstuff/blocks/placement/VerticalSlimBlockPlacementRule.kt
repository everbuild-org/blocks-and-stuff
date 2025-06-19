package org.everbuild.blocksandstuff.blocks.placement

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.RegistryTag
import org.everbuild.blocksandstuff.blocks.placement.util.States
import org.everbuild.blocksandstuff.common.tag.BlockTags
import java.util.Map
import kotlin.collections.plus

class VerticalSlimBlockPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val tagManager = Block.staticRegistry()
    private val leaves = tagManager.getTag(Key.key("minecraft:leaves"))!!
    private val shulkerBoxes = tagManager.getTag(Key.key("minecraft:shulker_boxes"))!!
    private val cannotConnect = RegistryTag.direct<Block>(
        shulkerBoxes.toList()
                + leaves.toList()
                + listOf(Block.BARRIER, Block.CARVED_PUMPKIN, Block.JACK_O_LANTERN, Block.MELON, Block.PUMPKIN)
    )

    private val walls = tagManager.getTag(Key.key("minecraft:walls"))!!
    private val glassPanes = BlockTags.getTaggedWith("blocksandstuff:glass_panes")
    private val canConnect = RegistryTag.direct<Block> (
        glassPanes.toList()
                + walls.toList()
                + listOf(Block.IRON_BARS)
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
        val isFaceFull = instanceBlock.registry().collisionShape().isFaceFull(blockFace)

        return !cannotConnect.contains(instanceBlock) && isFaceFull || canConnect.contains(instanceBlock) || instanceBlock.key() == this.block.key()
    }
}