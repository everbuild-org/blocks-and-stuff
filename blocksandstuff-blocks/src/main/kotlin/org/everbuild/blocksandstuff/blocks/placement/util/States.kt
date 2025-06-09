package org.everbuild.blocksandstuff.blocks.placement.util

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.utils.Direction
import java.util.*

object States {
    const val HALF: String = "half"
    const val FACING: String = "facing"
    const val FACE: String = "face"
    const val SHAPE: String = "shape"
    const val WATERLOGGED: String = "waterlogged"
    const val NORTH: String = "north"
    const val EAST: String = "east"
    const val SOUTH: String = "south"
    const val WEST: String = "west"

    fun getHalf(block: Block): BlockFace {
        if (block.getProperty(HALF) == null) return BlockFace.BOTTOM
        return BlockFace.valueOf(block.getProperty(HALF).uppercase(Locale.getDefault()))
    }

    fun getFacing(block: Block): BlockFace {
        if (block.getProperty(FACING) == null) return BlockFace.NORTH
        return BlockFace.valueOf(block.getProperty(FACING).uppercase(Locale.getDefault()))
    }

    fun getDirection(block: Block): Direction? {
        if (block.getProperty(FACE) == null) return Direction.NORTH
        return when (block.getProperty(FACE)) {
            "ceiling" -> Direction.DOWN
            "floor" -> Direction.UP
            else -> getFacing(block).toDirection()
        }
    }

    fun rotateYCounterclockwise(direction: Direction): Direction {
        return when (direction.ordinal) {
            2 -> Direction.WEST
            5 -> Direction.NORTH
            3 -> Direction.EAST
            4 -> Direction.SOUTH
            else -> throw IllegalStateException("Unable to rotate $direction")
        }
    }

    fun rotateYClockwise(direction: Direction): Direction {
        return when (direction.ordinal) {
            2 -> Direction.EAST
            5 -> Direction.SOUTH
            3 -> Direction.WEST
            4 -> Direction.NORTH
            else -> throw IllegalStateException("Unable to rotate $direction")
        }
    }

    fun getAxis(direction: Direction): Axis {
        return when (direction) {
            Direction.DOWN, Direction.UP -> Axis.Y
            Direction.NORTH, Direction.SOUTH -> Axis.Z
            Direction.WEST, Direction.EAST -> Axis.X
        }
    }

    enum class Axis {
        X,
        Y,
        Z
    }
}