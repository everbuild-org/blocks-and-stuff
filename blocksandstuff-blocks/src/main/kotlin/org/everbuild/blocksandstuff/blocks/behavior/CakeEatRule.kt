package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.registry.TagKey
import org.everbuild.blocksandstuff.blocks.event.CakeEatEvent

class CakeEatRule(val block: Block) : BlockHandler {
    private val maxSlices = 7
    private val candles = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:candles"))!!

    override fun getKey(): Key = block.key()

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) return super.onInteract(interaction)
        val currentSlices = interaction.block.getProperty("bites").toInt()
        if (currentSlices == 0 && candles.contains(
                interaction.player.getItemInHand(interaction.hand).material().block()
            )
        ) {
            return super.onInteract(interaction)
        }

        EventDispatcher.callCancellable(
            CakeEatEvent(
                interaction.player,
                interaction.block,
                BlockVec(interaction.blockPosition)
            )
        ) {
            if (currentSlices < (maxSlices - 1)) {
                interaction.instance.setBlock(
                    interaction.blockPosition,
                    interaction.block.withProperty("bites", (currentSlices + 1).toString())
                )
            } else {
                interaction.instance.setBlock(interaction.blockPosition, Block.AIR)
            }
        }
        return false
    }
}