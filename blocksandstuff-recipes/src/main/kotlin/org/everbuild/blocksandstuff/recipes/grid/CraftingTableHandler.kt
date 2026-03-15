package org.everbuild.blocksandstuff.recipes.grid

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.averium.org.everbuild.blocksandstuff.recipes.api.StashController

class CraftingTableHandler : BlockHandler {
    override fun getKey(): Key = Key.key("minecraft:crafting_table")

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) {
            return true
        }
        interaction.player.openInventory(CraftingTableInventory())
        return false
    }
}