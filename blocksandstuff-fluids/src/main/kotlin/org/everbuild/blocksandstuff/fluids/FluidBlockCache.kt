package org.everbuild.blocksandstuff.fluids

import net.minestom.server.instance.block.Block

object FluidBlockCache {
    val BLOCK_STATES by lazy {
        Block.staticRegistry()
            .values()
            .flatMap { it.possibleStates() }
            .filter {
                val isFluid = it.compare(Block.WATER, Block.Comparator.ID) || it.compare(Block.LAVA, Block.Comparator.ID)
                val isWaterlogged = (it.getProperty("waterlogged") ?: "false") == "true"
                isFluid || isWaterlogged
            }
            .map { it.stateId() }
            .toHashSet()
    }
}