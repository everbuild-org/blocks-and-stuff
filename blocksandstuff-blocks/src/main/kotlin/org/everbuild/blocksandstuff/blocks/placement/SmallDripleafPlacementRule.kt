package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.TagKey
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory.Companion.maybeDrop
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import org.everbuild.blocksandstuff.common.utils.isWater

class SmallDripleafPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val plantableOn = BlockTags.getTaggedWith("minecraft:small_dripleaf_placeable")
    private val dirtBlocks = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:dirt"))!!

    override fun blockPlace(placementState: PlacementState): Block {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        val blockAbove = placementState.instance.getBlock(placementState.placePosition.add(0.0, 1.0, 0.0))
        val currentBlock = placementState.instance.getBlock(placementState.placePosition)
        val direction = placementState.getNearestHorizontalLookingDirection()

        val bottomInsideWater = currentBlock.isWater()
        val topInsideWater = blockAbove.isWater()

        val instance = placementState.instance as Instance

        if (!blockAbove.isWater() && !blockAbove.isAir) return currentBlock

        if (plantableOn.any { it.compare(blockBelow) }) {
            setUpperBlock(
                instance,
                placementState.placePosition.add(0.0, 1.0, 0.0),
                direction.name.lowercase(),
                topInsideWater
            )

            return placementState.block
                .withProperty("waterlogged", bottomInsideWater.toString())
                .withProperty("facing", direction.name.lowercase())
                .withProperty("half", "lower")
        }

        if (!currentBlock.isWater()) return Block.AIR

        if (dirtBlocks.contains(blockBelow) || blockBelow.compare(Block.MUD) || blockBelow.compare(Block.FARMLAND)) {
            setUpperBlock(
                instance,
                placementState.placePosition.add(0.0, 1.0, 0.0),
                direction.name.lowercase(),
                topInsideWater
            )

            return placementState.block
                .withProperty("waterlogged", bottomInsideWater.toString())
                .withProperty("facing", direction.name.lowercase())
                .withProperty("half", "lower")
        }
        return currentBlock
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val belowBlock = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (!belowBlock.isSolid && !belowBlock.compare(Block.SMALL_DRIPLEAF)) {
            maybeDrop(updateState)
            if (updateState.currentBlock.getProperty("waterlogged")?.toBoolean() == true)
                return Block.WATER
            return Block.AIR
        }
        return updateState.currentBlock
    }

    private fun setUpperBlock(instance: Instance, position: Point, direction: String, waterlogged: Boolean) {
        instance.setBlock(
            position,
            Block.SMALL_DRIPLEAF
                .withProperty("facing", direction)
                .withProperty("half", "upper")
                .withProperty("waterlogged", waterlogged.toString())
            , false
        )
    }
}