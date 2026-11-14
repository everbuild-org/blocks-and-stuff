package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule

class TripwirePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        return placementState.block.withHorizontalProperties { face ->
            val neighborBlock = placementState.instance.getBlock(placementState.placePosition.relative(face))
            (neighborBlock.compare(Block.TRIPWIRE) || neighborBlock.compare(Block.TRIPWIRE_HOOK)).toString()
        }
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        return updateState.currentBlock.withHorizontalProperties { face ->
            val neighborBlock = updateState.instance.getBlock(updateState.blockPosition.relative(face))
            val connectedToTripwire = neighborBlock.compare(Block.TRIPWIRE)
            val connectedToHook =
                neighborBlock.compare(Block.TRIPWIRE_HOOK) &&
                    neighborBlock.getProperty("facing")!!.equals(face.oppositeFace.name.lowercase(), true)
            (connectedToTripwire || connectedToHook).toString()
        }
    }
}
