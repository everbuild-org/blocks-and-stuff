package org.everbuild.averium.org.everbuild.blocksandstuff.recipes.api

import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack

interface StashController {
    fun add(player: Player, item: ItemStack)
    fun empty(player: Player)
    fun sendNotification(player: Player)
    fun addToInventoryOrStash(player: Player, item: ItemStack)
}