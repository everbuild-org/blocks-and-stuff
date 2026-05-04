package org.everbuild.blocksandstuff.recipes.stonecutting

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.recipes.api.StashController
import org.everbuild.blocksandstuff.recipes.impl.StashControllerImpl

class StonecutterHandler(private val stashController: StashController = StashControllerImpl) : BlockHandler {
    override fun getKey(): Key = Key.key("minecraft:stonecutter")

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) {
            return true
        }

        interaction.player.openInventory(StonecutterInventory(stashController))
        return false
    }
}