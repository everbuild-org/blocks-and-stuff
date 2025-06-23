package org.everbuild.blocksandstuff.blocks.event

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.PlayerInstanceEvent
import net.minestom.server.instance.block.Block

class CakeEatEvent(
    private val player: Player,
    private val block: Block,
    private val blockPosition: BlockVec,
    private var cancelled: Boolean = false
) : Event, PlayerInstanceEvent, CancellableEvent, BlockEvent {
    override fun getPlayer(): Player = player
    override fun getBlock(): Block = block
    override fun getBlockPosition(): BlockVec = blockPosition

    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }
}