package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule

/** Player-placed leaves are persistent and therefore never decay. */
class LeavesPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block =
        placementState.block.withProperty("persistent", "true")
}
