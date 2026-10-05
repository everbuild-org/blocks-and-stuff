package org.everbuild.blocksandstuff.recipes.util

import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack

/**
 * Moves as much of [source] as possible into the given [slots] of [inventory].
 * Compatible stacks are merged first, then empty slots are filled.
 *
 * @return the part of [source] that could not be placed (AIR if everything fit)
 */
fun transferInto(
    source: ItemStack,
    inventory: Inventory,
    slots: List<Int>,
): ItemStack {
    var remaining = source

    for (slot in slots) {
        if (remaining.isAir) return ItemStack.AIR

        val existing = inventory.getItemStack(slot)
        if (existing.isAir || !existing.isSimilar(remaining)) continue

        val space = existing.maxStackSize() - existing.amount()
        if (space <= 0) continue

        val moved = minOf(space, remaining.amount())
        inventory.setItemStack(slot, existing.withAmount(existing.amount() + moved))
        remaining = remaining.withAmount(remaining.amount() - moved)
    }

    for (slot in slots) {
        if (remaining.isAir) return ItemStack.AIR
        if (!inventory.getItemStack(slot).isAir) continue

        val moved = minOf(remaining.maxStackSize(), remaining.amount())
        inventory.setItemStack(slot, remaining.withAmount(moved))
        remaining = remaining.withAmount(remaining.amount() - moved)
    }

    return remaining
}
