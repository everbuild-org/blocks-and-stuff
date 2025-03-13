package org.everbuild.blocksandstuff.blocks.group

import java.util.function.Function
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.group.block.AggregateTagBlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.BlockBlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.VanillaTagBlockGroup
import org.everbuild.blocksandstuff.blocks.group.placement.PlacementGroup
import org.everbuild.blocksandstuff.blocks.placement.RotatedPillarPlacementRule
import org.everbuild.blocksandstuff.blocks.placement.SlabPlacementRule

object VanillaPlacementRules {
    val ALL: ArrayList<PlacementGroup> = ArrayList()

    val ROTATED_PILLARS: PlacementGroup = group(
        all(
            byTag("minecraft:logs"),
            byBlock(Block.MUDDY_MANGROVE_ROOTS),
            byBlock(Block.BAMBOO_BLOCK),
            byBlock(Block.STRIPPED_BAMBOO_BLOCK),
            byBlock(Block.BASALT),
            byBlock(Block.POLISHED_BASALT),
            byBlock(Block.QUARTZ_PILLAR),
            byBlock(Block.PURPUR_PILLAR),
            byBlock(Block.BONE_BLOCK),
            byBlock(Block.DEEPSLATE),
            byBlock(Block.INFESTED_DEEPSLATE),
            byBlock(Block.OCHRE_FROGLIGHT),
            byBlock(Block.VERDANT_FROGLIGHT),
            byBlock(Block.PEARLESCENT_FROGLIGHT)
        ),
        ::RotatedPillarPlacementRule
    )

    val SLAB: PlacementGroup = group(
        byTag("minecraft:slabs"),
        ::SlabPlacementRule
    )

    private fun group(blockGroup: BlockGroup, valueFunction: Function<Block, BlockPlacementRule>): PlacementGroup {
        val result = PlacementGroup(blockGroup, valueFunction)
        ALL.add(result)
        return result
    }

    private fun all(vararg blockGroups: BlockGroup): BlockGroup {
        return AggregateTagBlockGroup(*blockGroups)
    }

    private fun byTag(@KeyPattern tag: String): BlockGroup {
        return VanillaTagBlockGroup(Key.key(tag))
    }

    private fun byBlock(block: Block): BlockGroup {
        return BlockBlockGroup(block)
    }
}
