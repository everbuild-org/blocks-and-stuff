package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag

class WoodenTrapDoorOpenRule(private val block: Block?) : BlockHandler {
    override fun getKey(): Key {
        return block?.key() ?: key.key()
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking && !interaction.player.itemInMainHand.isAir) return super.onInteract(interaction)
        var bool = interaction.block.getProperty("open")
        if (bool.equals("true")) {
            bool = "false"
        } else {
            bool = "true"
        }
        interaction.instance.setBlock(interaction.blockPosition, interaction.block.withProperty("open", bool))
        return false
    }
}