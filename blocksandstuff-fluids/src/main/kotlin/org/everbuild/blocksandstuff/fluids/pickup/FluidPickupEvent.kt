package org.everbuild.blocksandstuff.fluids.pickup

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.event.trait.PlayerEvent
import net.minestom.server.event.trait.PlayerInstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

class FluidPickupEvent(
    private val instance: Instance,
    private val player: Player,
    val sourceBlock: Block,
    val sourceBlockPosition: BlockVec,
    var blockToPlace: Block
) : CancellableEvent, PlayerInstanceEvent {
    private var isCancelled = false

    override fun isCancelled(): Boolean =isCancelled
    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    override fun getInstance(): Instance = instance
    override fun getPlayer(): Player = player
}