package org.everbuild.averium.org.everbuild.blocksandstuff.recipes.impl

import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import org.everbuild.averium.org.everbuild.blocksandstuff.recipes.api.StashController

object StashControllerImpl : StashController {
    override fun add(player: Player, item: ItemStack) {
        player.inventory.addItemStack(item)
    }

    override fun empty(player: Player) {}

    override fun sendNotification(player: Player) {
        player.sendMessage("Your stash is empty")
    }

    override fun addToInventoryOrStash(
        player: Player,
        item: ItemStack
    ) {
        player.inventory.addItemStack(item)
    }

}