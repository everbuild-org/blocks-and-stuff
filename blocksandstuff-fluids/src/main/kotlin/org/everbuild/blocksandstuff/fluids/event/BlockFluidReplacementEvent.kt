package org.everbuild.blocksandstuff.fluids.event

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

class BlockFluidReplacementEvent(
    val inst: Instance,
    val blk: Block,
    val pos: BlockVec
) : BlockEvent, InstanceEvent, CancellableEvent {
    private var isCancelled = true

    override fun getBlock(): Block = blk
    override fun getBlockPosition(): BlockVec = pos
    override fun getInstance(): Instance = inst

    override fun isCancelled(): Boolean = isCancelled
    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }
}