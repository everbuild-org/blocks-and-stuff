package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class PaleMossCarpetPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0)).air()) return null
        return withConnections(placementState.instance, placementState.placePosition, placementState.block)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0)).air()) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return withConnections(updateState.instance, updateState.blockPosition, updateState.currentBlock)
    }

    private fun withConnections(
        instance: Block.Getter,
        position: net.minestom.server.coordinate.Point,
        block: Block,
    ): Block {
        val connections =
            Direction.HORIZONTAL.mapNotNull { direction ->
                val face = BlockFace.fromDirection(direction)
                face.takeIf {
                    instance.getBlock(position.relative(face)).collisionShape().isFaceFull(face.oppositeFace)
                }
            }.toSet()
        return block.withHorizontalProperties { face -> if (face in connections) "low" else "none" }
    }
}
