package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule

class TurtleEggPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val current = placementState.instance.getBlock(placementState.placePosition)
        val amount = ((if (current.compare(block, Block.Comparator.ID))
            current.getProperty("eggs")!!.toIntOrNull() ?: 0
        else 0) + 1).coerceAtMost(4)

        return placementState.block.withProperty("eggs", amount.toString())
    }

    override fun isSelfReplaceable(replacement: Replacement): Boolean {
        val amount = if (replacement.block.compare(block, Block.Comparator.ID))
            replacement.block.getProperty("eggs")!!.toIntOrNull() ?: 0
        else 0

        return amount < 4
    }
}