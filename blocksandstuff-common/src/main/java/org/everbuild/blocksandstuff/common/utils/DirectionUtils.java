package org.everbuild.blocksandstuff.common.utils;

import net.minestom.server.utils.Direction;

public class DirectionUtils {
    public static String getAxis(Direction direction) {
        return switch (direction) {
            case UP, DOWN -> "y";
            case NORTH, SOUTH -> "z";
            case EAST, WEST -> "x";
        };
    }
}
