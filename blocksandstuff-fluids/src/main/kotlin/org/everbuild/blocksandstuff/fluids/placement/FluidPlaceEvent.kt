package org.everbuild.blocksandstuff.fluids.placement

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Player
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.PlayerInstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

class FluidPlaceEvent(
    private val instance: Instance,
    private val player: Player,
    val block: Block,
    val placePosition: BlockVec,
    var blockToPlace: Block
) : CancellableEvent, PlayerInstanceEvent {
    private var isCancelled = false

    override fun isCancelled(): Boolean = isCancelled
    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    override fun getInstance(): Instance = instance
    override fun getPlayer(): Player = player
}