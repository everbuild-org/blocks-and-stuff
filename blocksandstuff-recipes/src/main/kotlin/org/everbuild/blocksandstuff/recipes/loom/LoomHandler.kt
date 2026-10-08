package org.everbuild.blocksandstuff.recipes.loom

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler

class LoomHandler : BlockInventoryHandler() {
    override fun getKey(): Key = Key.key("minecraft:loom")

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) {
            return true
        }
        val inventory = LoomInventory()
        trackInventory(inventory, interaction.blockPosition)
        interaction.player.openInventory(inventory)
        return false
    }
}
