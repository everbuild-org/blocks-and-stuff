package org.everbuild.blocksandstuff.common.utils

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace

/** Resolves a block's vanilla `facing` property to its corresponding block face. */
fun blockFacing(block: Block): BlockFace? =
    when (block.getProperty("facing")) {
        "north" -> BlockFace.NORTH
        "south" -> BlockFace.SOUTH
        "east" -> BlockFace.EAST
        "west" -> BlockFace.WEST
        "up" -> BlockFace.TOP
        "down" -> BlockFace.BOTTOM
        else -> null
    }
