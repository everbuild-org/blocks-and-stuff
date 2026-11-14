package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace

private val horizontalFaces = listOf(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH)

internal fun Block.withHorizontalProperties(value: (BlockFace) -> String): Block =
    withProperties(horizontalFaces.associate { it.name.lowercase() to value(it) })
