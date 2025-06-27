package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block

class GrindstonePlacementRule(block: Block): FacedFacingPlacementRule(block) {
    override fun needSupport(): Boolean {
        return false
    }
}