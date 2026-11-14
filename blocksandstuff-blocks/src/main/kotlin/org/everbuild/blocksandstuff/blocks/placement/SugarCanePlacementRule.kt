package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.isWater

class SugarCanePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (!isSupported(placementState.instance, placementState.placePosition)) return null
        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (!isSupported(updateState.instance, updateState.blockPosition)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }

    private fun isSupported(
        instance: Block.Getter,
        blockPosition: Point,
    ): Boolean {
        val posBelow = blockPosition.sub(0.0, 1.0, 0.0)
        val below = instance.getBlock(posBelow)
        if (below.key().value() == "sugar_cane") return true
        if (below.key().value() !in PLANTABLE) return false

        for ((x, z) in VON_NEUMANN) {
            val atCane = blockPosition.add(x.toDouble(), 0.0, z.toDouble())
            if (isWaterOrFrost(instance.getBlock(atCane))) return true
            val atGround = posBelow.add(x.toDouble(), 0.0, z.toDouble())
            if (isWaterOrFrost(instance.getBlock(atGround))) return true
        }

        return false
    }

    private fun isWaterOrFrost(block: Block): Boolean = block.isWater() || block.compare(Block.FROSTED_ICE)

    companion object {
        val VON_NEUMANN = setOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        private val PLANTABLE =
            setOf(
                "grass_block",
                "dirt",
                "coarse_dirt",
                "podzol",
                "mycelium",
                "rooted_dirt",
                "moss_block",
                "mud",
                "muddy_mangrove_roots",
                "sand",
                "red_sand",
                "suspicious_sand",
            )
    }
}
