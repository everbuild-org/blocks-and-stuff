package org.everbuild.blocksandstuff.blocks.placement.common

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

abstract class AbstractRailPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockUpdate(updateState: UpdateState): Block {
        if (!isSupported(updateState.instance, updateState.blockPosition)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return super.blockUpdate(updateState)
    }

    protected fun getFixedPlacement(placementState: PlacementState): FixedPlacementResult {
        val fixedSidesNorthSouth = getFixedSides(placementState.instance, placementState.placePosition, RailShape.NORTH_SOUTH)
        val fixedSidesEastWest = getFixedSides(placementState.instance, placementState.placePosition, RailShape.EAST_WEST)
        if (fixedSidesNorthSouth.count() == 2) {
            return FixedPlacementResult.DefinitiveBlock(placementState.block.withProperty("shape", RailShape.NORTH_SOUTH.toString()))
        } else if (fixedSidesEastWest.count() == 2) {
            return FixedPlacementResult.DefinitiveBlock(placementState.block.withProperty("shape", RailShape.EAST_WEST.toString()))
        }

        val lockedDirection = if (fixedSidesNorthSouth.isNotEmpty()) fixedSidesNorthSouth.first().toDirection()
        else if (fixedSidesEastWest.isNotEmpty()) fixedSidesEastWest.first().toDirection()
        else null

        return FixedPlacementResult.LockedDirection(lockedDirection)
    }

    protected fun isSupported(instance: Block.Getter, blockPos: Point): Boolean {
        return instance
            .getBlock(blockPos.sub(0.0, 1.0, 0.0))
            .registry()!!
            .collisionShape()
            .isFaceFull(BlockFace.BOTTOM)
    }

    protected fun createSidedConnection(placementState: PlacementState, rotated: Direction, lockedDirection: Direction?): Block? {
        getSideConnection(
            placementState.instance,
            placementState.placePosition,
            BlockFace.fromDirection(rotated)
        )?.let { shape ->
            val sidePos = placementState.placePosition.add(rotated.vec())
            val sideBlock = placementState.instance.getBlock(sidePos)
            if (!sideBlock.compare(Block.RAIL, Block.Comparator.ID) && !shape.isStraight()) return@let
            val ownShape =
                RailShape.fromSides(listOfNotNull(lockedDirection, rotated).map { BlockFace.fromDirection(it) })
            (placementState.instance as Instance).setBlock(
                sidePos,
                sideBlock.withProperty("shape", shape.toString())
            )
            return placementState.block.withProperty("shape", ownShape.toString())
        }

        return null
    }

    protected fun getSideConnection(instance: Block.Getter, point: Point, side: BlockFace): RailShape? {
        val sidePos = point.add(side.toDirection().vec())
        val sideBlock = instance.getBlock(sidePos)
        val shape = sideBlock.getProperty("shape")?.let { RailShape.fromString(it) } ?: return null
        val fixed = getFixedSides(instance, sidePos, shape)
        if (fixed.count() == 2) return null
        return if (fixed.isEmpty()) {
            // 0 bounds, rotate rail
            when (side) {
                BlockFace.NORTH, BlockFace.SOUTH -> RailShape.NORTH_SOUTH
                BlockFace.EAST, BlockFace.WEST -> RailShape.EAST_WEST
                else -> null
            }
        } else {
            // 1 bound, other is free to be rotated to us
            RailShape.fromSides(listOf(side.oppositeFace, fixed.first()))
        }
    }

    protected fun getFixedSides(instance: Block.Getter, point: Point, shape: RailShape): List<BlockFace> {
        return shape.sides.filter { side ->
            val neighborBlock = instance.getBlock(point.add(side.toDirection().vec()))
            val neighborShape = neighborBlock.getProperty("shape")?.let { RailShape.fromString(it) }
            val directNeighbour = neighborShape != null && neighborShape.sides.contains(side.oppositeFace)

            val lowerNeighborBlock = instance.getBlock(point.add(side.toDirection().vec().sub(0.0, 1.0, 0.0)))
            val lowerNeighborShape = lowerNeighborBlock.getProperty("shape")?.let { RailShape.fromString(it) }
            val lowerNeighbor = lowerNeighborShape != null && lowerNeighborShape.sides.contains(side.oppositeFace) && lowerNeighborShape.isAscending()

            directNeighbour || lowerNeighbor
        }
    }

    protected fun connectVertical(
        shape: RailShape,
        placementState: PlacementState
    ): RailShape {
        var mutShape = shape
        for (face in mutShape.sides) {
            val position = placementState.placePosition.add(face.toDirection().vec().add(0.0, 1.0, 0.0))
            val upperShape =
                placementState.instance.getBlock(position).getProperty("shape")?.let { RailShape.fromString(it) }
                    ?: continue
            if (!upperShape.sides.any { mutShape.sides.contains(it) }) continue
            // vertical placement
            mutShape = RailShape.getAscendingTowards(face)
        }

        //update verticals below
        for (face in mutShape.sides) {
            val position = placementState.placePosition.add(face.toDirection().vec().add(0.0, -1.0, 0.0))
            val lowerBlock = placementState.instance.getBlock(position)
            val lowerShape = lowerBlock.getProperty("shape")?.let { RailShape.fromString(it) } ?: continue
            if (!lowerShape.sides.any { mutShape.sides.contains(it) }) continue
            if (!lowerShape.isStraight() || lowerShape.isAscending()) continue
            (placementState.instance as Instance).setBlock(
                position,
                lowerBlock
                    .withProperty("shape", RailShape.getAscendingTowards(face.oppositeFace).toString())
            )
        }
        return mutShape
    }

    enum class RailShape(val sides: List<BlockFace> = emptyList()) {
        NORTH_SOUTH(BlockFace.NORTH, BlockFace.SOUTH),
        EAST_WEST(BlockFace.EAST, BlockFace.WEST),
        NORTH_EAST(BlockFace.NORTH, BlockFace.EAST),
        NORTH_WEST(BlockFace.NORTH, BlockFace.WEST),
        SOUTH_EAST(BlockFace.SOUTH, BlockFace.EAST),
        SOUTH_WEST(BlockFace.SOUTH, BlockFace.WEST),
        ASCENDING_EAST(BlockFace.EAST, BlockFace.WEST),
        ASCENDING_WEST(BlockFace.WEST, BlockFace.EAST),
        ASCENDING_NORTH(BlockFace.NORTH, BlockFace.SOUTH),
        ASCENDING_SOUTH(BlockFace.SOUTH, BlockFace.NORTH),
        ;

        constructor(vararg sides: BlockFace) : this(sides.toList())

        override fun toString(): String {
            return super.toString().lowercase()
        }

        fun isAscending(): Boolean = when (this) {
            ASCENDING_EAST, ASCENDING_WEST, ASCENDING_NORTH, ASCENDING_SOUTH -> true
            else -> false
        }

        fun isStraight(): Boolean = when (this) {
            NORTH_SOUTH, EAST_WEST, ASCENDING_EAST, ASCENDING_WEST, ASCENDING_NORTH, ASCENDING_SOUTH -> true
            else -> false
        }

        companion object {
            fun fromString(value: String): RailShape? {
                return try {
                    valueOf(value.uppercase())
                } catch (_: IllegalArgumentException) {
                    null
                }
            }

            fun fromSides(requiredSides: List<BlockFace>): RailShape? {
                return entries.filter { shape ->
                    shape.sides.containsAll(requiredSides)
                }.minByOrNull { it.ordinal }
            }

            fun getAscendingTowards(face: BlockFace): RailShape {
                return when (face) {
                    BlockFace.NORTH -> ASCENDING_NORTH
                    BlockFace.SOUTH -> ASCENDING_SOUTH
                    BlockFace.EAST -> ASCENDING_EAST
                    BlockFace.WEST -> ASCENDING_WEST
                    else -> throw IllegalArgumentException("Only horizontal faces are supported")
                }
            }
        }
    }

    sealed interface FixedPlacementResult {
        @JvmInline
        value class DefinitiveBlock(val block: Block) : FixedPlacementResult
        @JvmInline
        value class LockedDirection(val direction: Direction?) : FixedPlacementResult
    }
}