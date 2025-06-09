package org.everbuild.blocksandstuff.common.utils

import net.minestom.server.instance.block.Block

fun Block.isWaterSource() = this.compare(Block.WATER)

fun Block.isWater() = this.compare(Block.WATER) || this.compare(Block.BUBBLE_COLUMN)