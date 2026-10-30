package org.everbuild.blocksandstuff.blocks.event

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.PlayerInstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

/** Emitted after a sculk shrieker blames [player] and raises their warden warning level. */
class WardenBlameEvent(
    private val player: Player,
    private val block: Block,
    private val blockPosition: BlockVec,
    private val instance: Instance,
    private val warningLevel: Int,
) : Event,
    PlayerInstanceEvent,
    BlockEvent {
    override fun getPlayer(): Player = player

    override fun getBlock(): Block = block

    override fun getBlockPosition(): BlockVec = blockPosition

    override fun getInstance(): Instance = instance

    /** Warning level after the blame was applied (0..4). */
    fun getWarningLevel(): Int = warningLevel
}
