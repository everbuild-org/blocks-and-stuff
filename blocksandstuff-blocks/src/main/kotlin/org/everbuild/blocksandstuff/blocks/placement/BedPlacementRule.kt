package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class BedPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val direction = placementState.getNearestHorizontalLookingDirection().opposite()
        val additionalReplacementBlock = placementState.placePosition.add(direction.vec())
        if (!placementState.instance.getBlock(additionalReplacementBlock).registry().isReplaceable) {
            return null
        }

        val instance = placementState.instance as Instance

        instance.setBlock(
            additionalReplacementBlock,
            placementState.block
                .withProperty("facing", direction.name.lowercase())
                .withProperty("part", "head")
        )

        return placementState.block
            .withProperty("facing", direction.name.lowercase())
            .withProperty("part", "foot")
    }
}