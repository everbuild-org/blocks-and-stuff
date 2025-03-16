package org.everbuild.blocksandstuff.blocks.group

import java.util.function.Function
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.group.block.AggregateTagBlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.BlockBlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.BlockGroup
import org.everbuild.blocksandstuff.blocks.group.block.TagBlockGroup
import org.everbuild.blocksandstuff.blocks.group.placement.PlacementGroup
import org.everbuild.blocksandstuff.blocks.placement.AmethystPlacementRules
import org.everbuild.blocksandstuff.blocks.placement.BambooPlantPlacementRule
import org.everbuild.blocksandstuff.blocks.placement.InverseWorkstationPlacementRule
import org.everbuild.blocksandstuff.blocks.placement.RotatedPillarPlacementRule
import org.everbuild.blocksandstuff.blocks.placement.SlabPlacementRule
import org.everbuild.blocksandstuff.blocks.placement.WorkstationPlacementRule

object VanillaPlacementRules {
    val ALL: ArrayList<PlacementGroup> = ArrayList()

    val ROTATED_PILLARS = group(
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
            byBlock(Block.PEARLESCENT_FROGLIGHT),
            byBlock(Block.HAY_BLOCK),
        ),
        ::RotatedPillarPlacementRule
    )

    val SLAB = group(
        byTag("minecraft:slabs"),
        ::SlabPlacementRule
    )

    val WORKSTATIONS = group(
        all(
            byBlock(Block.FURNACE),
            byBlock(Block.BLAST_FURNACE),
            byBlock(Block.SMOKER),
            byBlock(Block.LECTERN),
            byBlock(Block.ENDER_CHEST),
            byBlock(Block.CHISELED_BOOKSHELF),
            byBlock(Block.CARVED_PUMPKIN),
            byBlock(Block.JACK_O_LANTERN),
            byBlock(Block.BEEHIVE),
            byBlock(Block.STONECUTTER),
            byBlock(Block.LOOM),
            byBlock(Block.BEE_NEST),
            byBlock(Block.END_PORTAL_FRAME),
        ),
        ::WorkstationPlacementRule
    )

    val ROTATED_WORKSTATIONS = group(
        all(
            byBlock(Block.ANVIL),
            byBlock(Block.CHIPPED_ANVIL),
            byBlock(Block.DAMAGED_ANVIL),
        ),
        ::InverseWorkstationPlacementRule
    )

    val AMETHYST = group(
        all(
            byBlock(Block.AMETHYST_CLUSTER),
            byBlock(Block.SMALL_AMETHYST_BUD),
            byBlock(Block.MEDIUM_AMETHYST_BUD),
            byBlock(Block.LARGE_AMETHYST_BUD),
        ),
        ::AmethystPlacementRules
    )

    val BAMBOO = group(
        all(
            byBlock(Block.BAMBOO),
            byBlock(Block.BAMBOO_SAPLING),
        ),
        ::BambooPlantPlacementRule
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
        return TagBlockGroup(Key.key(tag))
    }

    private fun byBlock(block: Block): BlockGroup {
        return BlockBlockGroup(block)
    }
}
