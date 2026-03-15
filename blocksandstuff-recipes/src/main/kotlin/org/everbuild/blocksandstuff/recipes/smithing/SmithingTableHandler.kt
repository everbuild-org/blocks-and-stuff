package org.everbuild.blocksandstuff.recipes.smithing

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler

class SmithingTableHandler : BlockHandler {
    override fun getKey(): Key = Key.key("minecraft:smithing_table")

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) {
            return true
        }
        interaction.player.openInventory(SmithingTableInventory())
        return false
    }
}