package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getNearestLookingDirection

class ObserverPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        if (placementState.playerPosition() == null) return placementState.block()
        return placementState.block()
            .withProperty("facing", placementState.getNearestLookingDirection().opposite().name.lowercase())
    }
}