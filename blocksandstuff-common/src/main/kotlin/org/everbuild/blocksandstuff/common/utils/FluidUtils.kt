package org.everbuild.blocksandstuff.common.utils

import net.minestom.server.instance.block.Block

fun Block.isWater() = this.compare(Block.WATER) || this.compare(Block.BUBBLE_COLUMN)