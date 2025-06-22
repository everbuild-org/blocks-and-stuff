package org.everbuild.blocksandstuff.blocks.behavior

import kotlin.collections.component1
import kotlin.collections.component2
import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.blocks.event.CakeEatEvent
import org.everbuild.blocksandstuff.blocks.placement.CandlePlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler

class CandleCakeRule(val block: Block) : BlockHandler {
    override fun getKey(): Key = block.key()

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) return super.onInteract(interaction)
        EventDispatcher.callCancellable(
            CakeEatEvent(
                interaction.player,
                interaction.block,
                BlockVec(interaction.blockPosition)
            )
        ) {
            interaction.instance.setBlock(
                interaction.blockPosition,
                Block.CAKE
                    .withDefaultHandler()
                    .withProperty("bites", "1")
            )
            DroppedItemFactory.maybeDrop(interaction.instance, interaction.blockPosition, CAKE_CANDLE[block] ?: return@callCancellable)
        }
        return false
    }

    companion object {
        val CAKE_CANDLE = CandlePlacementRule.CANDLE_CAKE.entries.associate { (k, v) -> v to k }
    }
}
