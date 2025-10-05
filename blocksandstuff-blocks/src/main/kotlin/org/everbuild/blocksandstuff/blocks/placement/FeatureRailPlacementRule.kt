package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.blocks.placement.common.AbstractRailPlacementRule
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class FeatureRailPlacementRule(block: Block) : AbstractRailPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (!isSupported(placementState.instance, placementState.placePosition)) return null
        val primaryDirection = placementState.getNearestHorizontalLookingDirection()

        val lockedDirection = when(val fixed = getFixedPlacement(placementState)) {
            is FixedPlacementResult.DefinitiveBlock -> return fixed.block
            is FixedPlacementResult.LockedDirection  -> fixed.direction
        }

        val face = BlockFace.fromDirection(lockedDirection ?: primaryDirection)
        for (rotated in RailShape.entries.filter { it.isStraight() && !it.isAscending() && it.sides.contains(face) }.flatMap { it.sides }) {
            createSidedConnection(placementState, rotated.toDirection(), lockedDirection)?.let { return it }
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