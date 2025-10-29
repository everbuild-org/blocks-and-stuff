package org.everbuild.blocksandstuff.fluids

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler

class BubbleColumnPlacementRule : BlockPlacementRule(Block.BUBBLE_COLUMN) {
    override fun blockPlace(placementState: PlacementState): Block = placementState.block

    override fun blockUpdate(updateState: UpdateState): Block? {
        val bottomBlock = updateState.instance.getBlock(updateState.blockPosition.sub(0.0, 1.0, 0.0))
        return if (bottomBlock.compare(
                Block.SOUL_SAND,
                Block.Comparator.ID
            ) || (
                    bottomBlock.compare(Block.BUBBLE_COLUMN, Block.Comparator.ID)
                            && bottomBlock.getProperty("drag") == "false"
                    )
        ) {
            Block.BUBBLE_COLUMN
                .withDefaultHandler()
                .withProperty("drag", "false")
        } else if (
            bottomBlock.compare(Block.MAGMA_BLOCK, Block.Comparator.ID)
            || (
                    bottomBlock.compare(Block.BUBBLE_COLUMN, Block.Comparator.ID)
                            && bottomBlock.getProperty("drag") == "true"
                    )
        ) {
            Block.BUBBLE_COLUMN
                .withDefaultHandler()
                .withProperty("drag", "true")
        } else {
            Block.WATER.withDefaultHandler()
        }
    }
}