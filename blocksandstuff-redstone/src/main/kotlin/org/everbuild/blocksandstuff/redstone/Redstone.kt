package org.everbuild.blocksandstuff.redstone

import net.minestom.server.instance.block.BlockFace

/** Highest signal strength a component can emit. */
const val REDSTONE_MAX_POWER: Int = 15

/** All six axis-aligned faces. */
val ALL_FACES: Set<BlockFace> = BlockFace.entries.toSet()
