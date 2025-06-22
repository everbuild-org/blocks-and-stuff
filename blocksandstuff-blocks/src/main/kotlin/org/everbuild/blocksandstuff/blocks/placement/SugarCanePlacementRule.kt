package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.RegistryTag
import net.minestom.server.registry.TagKey
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.isWater

class SugarCanePlacementRule(block: Block) : BlockPlacementRule(block) {
    private val blockRegistry = Block.staticRegistry()
    private val dirt = blockRegistry.getTag(TagKey.ofHash("#minecraft:dirt"))!!
    private val sand = blockRegistry.getTag(TagKey.ofHash("#minecraft:sand"))!!
    private val plantable = RegistryTag.direct(
        *dirt.toList().toTypedArray(),
        *sand.toList().toTypedArray(),
    )

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

    private fun isSupported(instance: Block.Getter, blockPosition: Point): Boolean {
        val posBelow = blockPosition.sub(0.0, 1.0, 0.0)
        val below = instance.getBlock(posBelow)
        if (below.compare(block, Block.Comparator.ID)) return true
        if (!plantable.contains(below)) return false

        for ((x, z) in VON_NEUMANN) {
            val pos = posBelow.add(x.toDouble(), 0.0, z.toDouble())
            val block = instance.getBlock(pos)
            if (block.isWater()) return true
        }

        return false
    }

    companion object {
        val VON_NEUMANN = setOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
    }
}