package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
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

    override fun blockUpdate(updateState: UpdateState): Block {
        val facing = BlockFace.fromDirection(
            Direction.valueOf(
                updateState.currentBlock
                    .getProperty("facing")
                    .uppercase()
            )
        )
        val (neighbourFacing, neighbourPart) = if (updateState.currentBlock.getProperty("part") == "foot") {
            facing to "head"
        } else {
            facing.oppositeFace to "foot"
        }

        val neighbour = updateState.instance.getBlock(updateState.blockPosition.relative(neighbourFacing))
        if (!neighbour.compare(block, Block.Comparator.ID)) {
            return Block.AIR
        }

        val realNeighbourPart = neighbour.getProperty("part")
        if (realNeighbourPart != neighbourPart) {
            return Block.AIR
        }

        return updateState.currentBlock
    }
}