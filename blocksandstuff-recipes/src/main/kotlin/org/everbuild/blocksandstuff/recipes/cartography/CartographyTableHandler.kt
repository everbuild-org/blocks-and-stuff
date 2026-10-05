package org.everbuild.blocksandstuff.recipes.cartography

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler

class CartographyTableHandler : BlockInventoryHandler() {
    override fun getKey(): Key = Key.key("minecraft:cartography_table")

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) {
            return true
        }
        val inventory = CartographyTableInventory()
        trackInventory(inventory, interaction.blockPosition)
        interaction.player.openInventory(inventory)
        return false
    }
}
