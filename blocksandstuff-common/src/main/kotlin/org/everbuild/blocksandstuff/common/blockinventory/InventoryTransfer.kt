package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.item.ItemStack

/**
 * Moves as much of [stack] as possible into [slots], merging compatible stacks first.
 *
 * @return the part of [stack] that could not be inserted (`AIR` when everything fit)
 */
fun PhysicalInventory.insertStack(
    stack: ItemStack,
    slots: Collection<Int> = insertableSlots,
): ItemStack {
    if (stack.isAir) return ItemStack.AIR

    var remaining = stack
    transact { set ->
        for (slot in slots) {
            if (remaining.isAir) return@transact

            val existing = getItemStack(slot)
            if (existing.isAir || !existing.isSimilar(remaining)) continue

            val space = existing.maxStackSize() - existing.amount()
            if (space <= 0) continue

            val moved = minOf(space, remaining.amount())
            set(slot, existing.withAmount(existing.amount() + moved))
            remaining = remaining.withAmount(remaining.amount() - moved)
        }

        for (slot in slots) {
            if (remaining.isAir) return@transact
            if (!getItemStack(slot).isAir) continue

            val moved = minOf(remaining.maxStackSize(), remaining.amount())
            set(slot, remaining.withAmount(moved))
            remaining = remaining.withAmount(remaining.amount() - moved)
        }
    }
    return remaining
}

/** Removes a single item from the first non-empty slot in [slots]. */
fun PhysicalInventory.extractOne(slots: Collection<Int> = extractableSlots): ItemStack? {
    var extracted: ItemStack? = null
    transact { set ->
        for (slot in slots) {
            val current = getItemStack(slot)
            if (current.isAir) continue

            extracted = current.withAmount(1)
            set(slot, if (current.amount() > 1) current.withAmount(current.amount() - 1) else ItemStack.AIR)
            return@transact
        }
    }
    return extracted
}

/** Vanilla comparator output (0..15) derived from how full this inventory is. */
fun PhysicalInventory.comparatorSignal(): Int {
    val slots = archetype.size
    if (slots <= 0) return 0

    var fullness = 0.0
    for (slot in 0 until slots) {
        val stack = getItemStack(slot)
        if (stack.isAir) continue
        fullness += stack.amount().toDouble() / stack.maxStackSize()
    }
    if (fullness <= 0.0) return 0

    return (1.0 + 14.0 * (fullness / slots)).toInt().coerceIn(0, 15)
}
