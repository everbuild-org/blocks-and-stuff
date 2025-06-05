package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import org.everbuild.blocksandstuff.common.utils.isWater

class BigDripleafPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val plantableOn = BlockTags.getTaggedWith("minecraft:big_dripleaf_placeable")

    override fun blockPlace(placementState: PlacementState): Block? {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.sub(0.0, 1.0, 0.0))
        val placingInsideWater = placementState.instance.getBlock(placementState.placePosition).isWater()
        if (plantableOn.any { it.compare(blockBelow) }) {
            val direction = placementState.getNearestHorizontalLookingDirection()

            return Block.BIG_DRIPLEAF
                .withProperty("facing", direction.name.lowercase())
                .withProperty("waterlogged", placingInsideWater.toString())
        }

        if (blockBelow.compare(Block.BIG_DRIPLEAF)) {
            val direction = blockBelow.getProperty("facing")
            val bottomInsideWater = blockBelow.getProperty("waterlogged").toBoolean()
            val instance = placementState.instance as Instance

            instance.setBlock(
                placementState.placePosition.sub(0.0, 1.0, 0.0),
                Block.BIG_DRIPLEAF_STEM
                    .withProperty("facing", direction)
                    .withProperty("waterlogged", bottomInsideWater.toString())
            )

            return Block.BIG_DRIPLEAF
                .withProperty("facing", direction)
                .withProperty("waterlogged", placingInsideWater.toString())
        }

        return null
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val blockAbove = updateState.instance.getBlock(updateState.blockPosition.add(0.0, 1.0, 0.0))
        if (block.compare(Block.BIG_DRIPLEAF_STEM)
            && !(blockAbove.compare(Block.BIG_DRIPLEAF_STEM) || blockAbove.compare(Block.BIG_DRIPLEAF))
        ) {
            println("block above check failed: $blockAbove")
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        val blockBelow = updateState.instance.getBlock(updateState.blockPosition.sub(0.0, 1.0, 0.0))
        if (!blockBelow.compare(Block.BIG_DRIPLEAF_STEM) && plantableOn.none { it.compare(blockBelow) }) {
            println("block below check failed: $blockBelow ${!blockBelow.compare(Block.BIG_DRIPLEAF_STEM)} ${plantableOn.none { it.compare(blockBelow) }}")
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return updateState.currentBlock
    }
}