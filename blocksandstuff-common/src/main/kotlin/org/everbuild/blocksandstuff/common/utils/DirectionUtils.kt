package org.everbuild.blocksandstuff.common.utils

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