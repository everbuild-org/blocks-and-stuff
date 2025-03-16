package org.everbuild.blocksandstuff.common.utils

import kotlin.enums.EnumEntries
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.block.rule.BlockPlacementRule.PlacementState
import net.minestom.server.utils.Direction

fun Direction.getAxis(): String {
    return when (this) {
        Direction.UP, Direction.DOWN -> "y"
        Direction.NORTH, Direction.SOUTH -> "z"
        Direction.EAST, Direction.WEST -> "x"
    }
}

fun PlacementState.getHorizontalPlacementDirection(): Direction? {
    var yaw = this.playerPosition?.yaw ?: return null
    if (yaw < 0) yaw += 360f

    return when (yaw) {
        in 45f..135f -> Direction.EAST
        in 135f..225f -> Direction.SOUTH
        in 225f..315f -> Direction.WEST
        in 0f..45f -> Direction.NORTH
        else -> null
    }
}

fun PlacementState.sixteenStepRotation(): Int {
    var yaw = this.playerPosition?.yaw ?: return 0
    yaw += 22.5f / 2.0f
    if (yaw < 0) yaw += 360f
    return (yaw / 22.5f).toInt().coerceIn(0, 15)
}

fun PlacementState.canAttach(): Boolean {
    val anchor = this.placePosition.sub((this.blockFace ?: return false).toDirection().vec())
    val anchorBlock = this.instance.getBlock(anchor)
    return anchorBlock.registry().collisionShape().isFaceFull(this.blockFace!!)
}

fun PlacementState.getNearestLookingDirection(): Direction {
    this.playerPosition ?: return Direction.EAST
    val playerViewVector = this.playerPosition!!.direction().normalize()
    return Direction.entries.minBy { it.vec().scalarProduct(playerViewVector) }
}

fun Vec.scalarProduct(rhs: Vec): Double {
    val componentsMultiplied = this.mul(rhs)
    return componentsMultiplied.x + componentsMultiplied.y + componentsMultiplied.z
}

fun Direction.rotateR(): Direction {
    return when (this) {
        Direction.NORTH -> Direction.EAST
        Direction.EAST -> Direction.SOUTH
        Direction.SOUTH -> Direction.WEST
        Direction.WEST -> Direction.NORTH
        else -> this
    }
}
