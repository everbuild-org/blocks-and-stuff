package org.everbuild.blocksandstuff.recipes.smithing

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler
import org.everbuild.blocksandstuff.recipes.RecipeFactory

class SmithingTableHandler : BlockInventoryHandler() {
    override fun getKey(): Key = Key.key("minecraft:smithing_table")

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) {
            return true
        }
        val inventory = SmithingTableInventory()
        trackInventory(
            inventory,
            interaction.blockPosition,
            { player, item -> RecipeFactory.stashController.addToInventoryOrStash(player, item) },
        )
        interaction.player.openInventory(inventory)
        return false
    }
}
