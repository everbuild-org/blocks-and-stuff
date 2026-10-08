package org.everbuild.blocksandstuff.recipes.stonecutting

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler
import org.everbuild.blocksandstuff.recipes.api.StashController
import org.everbuild.blocksandstuff.recipes.impl.StashControllerImpl

class StonecutterHandler(private val stashController: StashController = StashControllerImpl) : BlockInventoryHandler() {
    override fun getKey(): Key = Key.key("minecraft:stonecutter")

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) {
            return true
        }

        val inventory = StonecutterInventory(stashController)
        trackInventory(inventory, interaction.blockPosition) { player, item ->
            stashController.addToInventoryOrStash(player, item)
        }
        interaction.player.openInventory(inventory)
        return false
    }
}
