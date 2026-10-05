package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
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
    private var cursorReturnEnabled = false

    override fun add(
        player: Player,
        item: ItemStack,
    ) {
        if (item.isAir) return

        val leftover = player.inventory.addItemStack(item, TransactionOption.ALL)
        if (leftover.isAir) return

        DroppedItemFactory.maybeDropFromPlayer(player, leftover)
    }

    /**
     * Minestom drops the cursor item before trying the player inventory when a
     * window is closed (see [net.minestom.server.inventory.AbstractInventory.removeViewer]).
     *
     * We want the opposite order: put the item into the player inventory first and
     * only drop what does not fit. Clearing the cursor before Minestom handles the
     * close makes its drop path a no-op.
     */
    fun enableCursorReturn() {
        if (cursorReturnEnabled) return
        cursorReturnEnabled = true

        MinecraftServer.getGlobalEventHandler().addListener(InventoryCloseEvent::class.java) { event ->
            // Window 0 is the player's own inventory and has no cursor to return.
            if (event.inventory.windowId.toInt() == 0) return@addListener

            val player = event.player
            val cursor = player.inventory.cursorItem
            if (cursor.isAir) return@addListener

            player.inventory.cursorItem = ItemStack.AIR
            add(player, cursor)
        }
    }
}
