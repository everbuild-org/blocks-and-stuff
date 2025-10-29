package org.everbuild.blocksandstuff.fluids

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.isWaterSource
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler

open class FluidPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val placedPoint = placementState.placePosition() // The actual placed position
        MinestomFluids.scheduleTick(
            placementState.instance() as Instance,
            placedPoint,
            block,
        )
        return block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val instance = updateState.instance() as Instance
        val point = updateState.blockPosition()
        val block = updateState.currentBlock()
        val bottomBlock = instance.getBlock(point.sub(0.0, 1.0, 0.0))

        // Schedule update for the current block
        MinestomFluids.scheduleTick(instance, point, block)

        // Schedule updates for adjacent blocks (ensures fluid spreads properly)
        for (face in BlockFace.entries) {
            val neighbor = point.relative(face)
            val neighborBlock = instance.getBlock(neighbor)
            if (MinestomFluids.getFluidOnBlock(neighborBlock) !== MinestomFluids.EMPTY) {
                MinestomFluids.scheduleTick(instance, neighbor, neighborBlock)
            }
        }

        if (block.isWaterSource()) {
            if (bottomBlock.compare(Block.SOUL_SAND, Block.Comparator.ID)) {
                return Block.BUBBLE_COLUMN
                    .withDefaultHandler()
                    .withProperty("drag", "false")
            } else if (bottomBlock.compare(Block.MAGMA_BLOCK, Block.Comparator.ID)) {
                return Block.BUBBLE_COLUMN
                    .withDefaultHandler()
                    .withProperty("drag", "true")
            }
        }
        return block
    }
}
