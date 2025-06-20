package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class GateOpenRule(private val block: Block?) : BlockHandler {
    override fun getKey(): Key {
        return block?.key() ?: key.key()
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking && !interaction.player.itemInMainHand.isAir)
            return true
        val direction = interaction.getNearestHorizontalLookingDirection().opposite()
        val bool = (!interaction.block.getProperty("open").toBoolean()).toString()
        interaction.instance.setBlock(interaction.blockPosition,
            interaction.block
                .withProperty("open", bool)
                .withProperty("facing", direction.toString().lowercase())
        )
        return false
    }
}