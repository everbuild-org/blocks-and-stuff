package org.everbuild.blocksandstuff.blocks.event

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.PlayerInstanceEvent
import net.minestom.server.instance.block.Block

class PlayerOpenSignEditorEvent(
    private val player: Player, private val blockPosition: BlockVec, private val block: Block
) : Event, BlockEvent, CancellableEvent, PlayerInstanceEvent {
    private var cancelled = false
    override fun getBlock(): Block = block
    override fun getBlockPosition(): BlockVec = blockPosition
    override fun getPlayer(): Player = player

    override fun isCancelled(): Boolean = cancelled
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }
}