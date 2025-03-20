package org.everbuild.blocksandstuff.fluids

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.BlockFace

fun raycastForFluid(player: Player, startPosition: Point, direction: Vec, maxDistance: Double): Point? {
    var currentPosition = startPosition
    val stepSize = 0.1 // Smaller steps give more precision, but increase computational cost
    var distance = 0.0

    while (distance < maxDistance) {
        currentPosition = currentPosition.add(direction.mul(stepSize))
        val block = player.instance.getBlock(currentPosition)

        if (block.isLiquid) {
            val levelStr = block.getProperty("level")
            if (levelStr.toInt() == 0)
                return currentPosition // Found a fluid block, return it
        }

        if (block.isSolid) {
            break // Hit a solid block, stop the raycast
        }

        distance += stepSize
    }


    return null // No fluid found within the range
}

fun findBlockFace(player: Player, blockPosition: Point): BlockFace? {
    return BlockFace.entries.firstOrNull { dir ->
        val direction = dir.toDirection()
        blockPosition.add(
            direction.normalX().toDouble(),
            direction.normalY().toDouble(),
            direction.normalZ().toDouble()
        ).isApproximatelyEqual(blockPosition.relative(dir))
    }
}

fun Point.isApproximatelyEqual(other: Point, epsilon: Double = 1e-6): Boolean {
    return (x() - other.x()).coerceAtLeast(-epsilon) <= epsilon &&
            (y() - other.y()).coerceAtLeast(-epsilon) <= epsilon &&
            (z() - other.z()).coerceAtLeast(-epsilon) <= epsilon
}