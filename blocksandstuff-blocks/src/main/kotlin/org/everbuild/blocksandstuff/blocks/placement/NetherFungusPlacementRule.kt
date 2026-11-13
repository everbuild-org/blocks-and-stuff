package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

/**
 * Crimson/warped fungi can be planted on most vegetation-supporting blocks, but only grow into a
 * huge fungus on their matching nylium. They pop off when they lose their support.
 */
class NetherFungusPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val below = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        return if (isValidBase(below)) placementState.block else null
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val below = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        if (!isValidBase(below)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }

    private fun isValidBase(block: Block): Boolean = block.key().value() in VALID_BASES

    companion object {
        private val VALID_BASES =
            setOf(
                "grass_block",
                "dirt",
                "coarse_dirt",
                "podzol",
                "farmland",
                "rooted_dirt",
                "moss_block",
                "crimson_nylium",
                "warped_nylium",
                "mycelium",
                "soul_soil",
                "mud",
                "muddy_mangrove_roots",
            )
    }
}

