package org.everbuild.blocksandstuff.fluids

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

internal class FluidPlacementRule(block: Block) : BlockPlacementRule(block) {
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
        return block
    }
}
