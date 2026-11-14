package org.everbuild.blocksandstuff.recipes.util

import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

internal fun Inventory.shiftClickToRoutedSlots(
    player: Player,
    slot: Int,
    resultSlot: Int,
    slotFor: (Material) -> Int?,
): Boolean {
    if (slot == resultSlot) return false
    val playerInventory = player.inventory
    if (slot < size) {
        val clicked = getItemStack(slot)
        if (clicked.isAir) return false

        setItemStack(slot, ItemStack.AIR)
        val leftover = playerInventory.addItemStack(clicked, TransactionOption.ALL)
        if (!leftover.isAir) DroppedItemFactory.maybeDropFromPlayer(player, leftover)

        playerInventory.update()
        update(player)
        return true
    }

    val clickSlot = slot - size
    val clicked = playerInventory.getItemStack(clickSlot)
    if (clicked.isAir) return false

    val target = slotFor(clicked.material()) ?: return false
    val leftover = transferInto(clicked, this, listOf(target))
    playerInventory.setItemStack(clickSlot, leftover)
    playerInventory.update()
    update(player)
    return true
}

internal fun Inventory.applyShiftClickResult(
    player: Player,
    clickSlot: Int,
    clicked: ItemStack,
    cursor: ItemStack,
    cancelled: Boolean,
): Boolean {
    val playerInventory = player.inventory
    if (cancelled) {
        playerInventory.update()
        update(player)
        return false
    }

    playerInventory.setItemStack(clickSlot, clicked)
    playerInventory.update()
    update(player)
    playerInventory.cursorItem = cursor
    return true
}
