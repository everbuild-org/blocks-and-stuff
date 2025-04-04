package org.everbuild.blocksandstuff.fluids

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

class WaterBlockBreakEvent(instance: Instance?, point: Point?, block: Block?) : InstanceEvent, BlockEvent, CancellableEvent {
    private var instance: Instance? = null
    private var blockPosition: Point? = null
    private var block: Block? = null

    private var cancelled = false

    fun waterBlockBreakEvent(instance: Instance, blockPosition: Point, block: Block) {
        this.instance = instance
        this.blockPosition = blockPosition
        this.block = block
    }

    override fun getInstance(): Instance {
        return instance!!
    }

    override fun getBlockPosition(): BlockVec {
        return blockPosition as BlockVec
    }

    override fun getBlock(): Block {
        return block!!
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }
}
