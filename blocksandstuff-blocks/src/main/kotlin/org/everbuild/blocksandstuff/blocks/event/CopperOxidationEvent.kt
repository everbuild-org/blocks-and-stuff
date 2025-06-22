package org.everbuild.blocksandstuff.blocks.event

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.Event
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

class CopperOxidationEvent(
    private val block: Block,
    private var blockAfterOxidation: Block,
    private val position: BlockVec,
    private val instance: Instance
) : Event, CancellableEvent, BlockEvent, InstanceEvent {
    private var cancelled = false

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    fun getBlockAfterOxidation(): Block = blockAfterOxidation
    fun setBlockAfterOxidation(block: Block) {
        blockAfterOxidation = block
    }

    override fun getBlock(): Block = block
    override fun getBlockPosition(): BlockVec = position
    override fun getInstance(): Instance = instance
}