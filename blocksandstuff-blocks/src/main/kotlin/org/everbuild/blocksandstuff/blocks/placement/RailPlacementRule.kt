package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.blocks.placement.common.AbstractRailPlacementRule
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class RailPlacementRule(block: Block) : AbstractRailPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (!isSupported(placementState.instance, placementState.placePosition)) return null

        val fixed = getFixedPlacement(placementState)
        val lockedDirection = when(fixed) {
            is FixedPlacementResult.DefinitiveBlock -> return fixed.block
            is FixedPlacementResult.LockedDirection  -> fixed.direction
        }

        val primaryDirection = placementState.getNearestHorizontalLookingDirection()

        for (rotated in Direction.HORIZONTAL.filter { it != lockedDirection }) {
            createSidedConnection(placementState, rotated, lockedDirection)?.let { return it }
        }

        val shape = connectVertical(RailShape.fromSides(
            listOf(
                BlockFace.fromDirection(lockedDirection ?: primaryDirection)
            )
        )!!, placementState)

        return placementState.block
            .withProperty(
                "shape",
                shape.toString()
            )
    }
}