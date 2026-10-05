package org.everbuild.blocksandstuff.recipes.grid

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler
import org.everbuild.blocksandstuff.common.blockinventory.ItemStash
import org.everbuild.blocksandstuff.recipes.RecipeFactory

class CraftingTableHandler : BlockInventoryHandler() {
    override fun getKey(): Key = Key.key("minecraft:crafting_table")

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) {
            return true
        }
        val inventory = CraftingTableInventory()
        trackInventory(inventory, interaction.blockPosition, stash())
        interaction.player.openInventory(inventory)
        return false
    }

    private fun stash() =
        ItemStash { player, item ->
            RecipeFactory.stashController.addToInventoryOrStash(player, item)
        }
}
