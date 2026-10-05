package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.entity.Player
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

/**
 * Abstraction over "where do items go when a container is closed".
 *
 * Implementations are responsible for fully consuming the item, either by
 * storing it (player inventory, custom stash, ...) or by dropping whatever
 * does not fit.
 */
fun interface ItemStash {
    fun add(
        player: Player,
        item: ItemStack,
    )
}

/**
 * Default stash: puts the item into the player's inventory and drops the
 * leftover in front of the player.
 */
object PlayerInventoryStash : ItemStash {
    override fun add(
        player: Player,
        item: ItemStack,
    ) {
        if (item.isAir) return

        val leftover = player.inventory.addItemStack(item, TransactionOption.ALL)
        if (leftover.isAir) return

        player.instance?.let { instance ->
            DroppedItemFactory.maybeDrop(instance, player.position, leftover)
        }
    }
}
