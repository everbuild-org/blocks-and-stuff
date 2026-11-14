package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.util.leavesDistance

/** Player-placed leaves are persistent and keep their vanilla distance-to-log property updated. */
class LeavesPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block =
        placementState.block
            .withProperty("persistent", "true")
            .withProperty(
                "distance",
                leavesDistance(placementState.instance, placementState.placePosition).toString(),
            )

    override fun blockUpdate(updateState: UpdateState): Block {
        val distance = leavesDistance(updateState.instance, updateState.blockPosition).toString()
        if (updateState.currentBlock.getProperty("distance") == distance) return updateState.currentBlock
        return updateState.currentBlock.withProperty("distance", distance)
    }
}
