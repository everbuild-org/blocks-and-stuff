package org.everbuild.blocksandstuff.blocks.randomticking

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

interface RandomTickHandler {

    /**
     * @return the new block to update, or null if no update is necessary
     */
    fun onRandomTick(randomTick: RandomTick): Block?

    data class RandomTick(
        val block: Block,
        val instance: Instance,
        val blockPosition: BlockVec
    )
}