package org.everbuild.blocksandstuff.fluids.event

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

/**
 * Fired when a fluid is replaced by a block
 * (ex. in the event that water and lava mix)
 */
class FluidBlockReplacementEvent(
    val inst: Instance,
    var blk: Block,
    val position: BlockVec
) : InstanceEvent, BlockEvent, CancellableEvent {
    private var isCancelled = false

    override fun getBlock(): Block = blk
    fun setBlock(block: Block) {
        this.blk = block
    }

    override fun getBlockPosition(): BlockVec = position
    override fun getInstance(): Instance = inst

    override fun isCancelled(): Boolean = isCancelled
    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }
}