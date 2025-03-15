package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getHorizontalPlacementDirection
import org.everbuild.blocksandstuff.common.utils.rotateR

class InverseWorkstationPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        return placementState.block
            .withProperty(
                "facing",
                (placementState.getHorizontalPlacementDirection()
                    ?: return placementState.block).rotateR().rotateR().rotateR().name.lowercase()
            )
    }
}
