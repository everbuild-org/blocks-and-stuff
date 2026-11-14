package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventory

/**
 * Vanilla forbids shulker boxes inside shulker boxes. Cancel the click before any item is moved so the
 * stack stays in its original slot instead of being dropped (or lost) after the fact.
 */
object ShulkerClickGuard {
    private const val OFFHAND_SLOT = 40
    private var enabled = false

    fun enable() {
        if (enabled) return
        enabled = true

        MinecraftServer.getGlobalEventHandler().addListener(InventoryPreClickEvent::class.java) { event ->
            val open = event.player.openInventory as? BlockInventory ?: return@addListener
            if (!open.rejectsShulkerBoxes) return@addListener
            if (wouldNestShulker(open, event)) event.isCancelled = true
        }
    }

    internal fun wouldNestShulker(
        open: BlockInventory,
        event: InventoryPreClickEvent,
    ): Boolean {
        val size = open.size
        val click = event.click
        val player = event.player
        val cursor = player.inventory.cursorItem

        return when (click) {
            is Click.Left, is Click.Right, is Click.Double -> event.slot < size && isShulker(cursor)
            is Click.LeftShift, is Click.RightShift ->
                event.slot >= size && isShulker(player.inventory.getItemStack(event.slot - size))
            is Click.LeftDrag, is Click.RightDrag, is Click.MiddleDrag ->
                click.slots().any { it < size } && isShulker(cursor)
            is Click.HotbarSwap -> click.slot < size && isShulker(player.inventory.getItemStack(click.hotbarSlot))
            is Click.OffhandSwap -> click.slot < size && isShulker(player.inventory.getItemStack(OFFHAND_SLOT))
            else -> false
        }
    }

    private fun isShulker(stack: ItemStack): Boolean {
        if (stack.isAir) return false
        val key = stack.material().key().value()
        return key == "shulker_box" || key.endsWith("_shulker_box")
    }
}
