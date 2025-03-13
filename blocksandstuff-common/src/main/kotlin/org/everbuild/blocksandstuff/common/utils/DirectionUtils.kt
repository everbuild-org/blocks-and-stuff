package org.everbuild.blocksandstuff.common.utils

import net.minestom.server.utils.Direction

val Direction.axis get(): String {
    return when (this) {
        Direction.UP, Direction.DOWN -> "y"
        Direction.NORTH, Direction.SOUTH -> "z"
        Direction.EAST, Direction.WEST -> "x"
    }
}
