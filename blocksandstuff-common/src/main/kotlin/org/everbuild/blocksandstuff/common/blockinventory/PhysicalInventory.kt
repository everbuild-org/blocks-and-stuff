package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.item.ItemStack

interface PhysicalInventory {
    val archetype: BlockInventoryArchetype

    /** Slots that may receive items from other inventories (default: all slots). */
    val insertableSlots: Collection<Int> get() = List(archetype.size) { it }

    /** Slots that may be drained by other inventories (default: all slots). */
    val extractableSlots: Collection<Int> get() = List(archetype.size) { it }

    fun getItemStack(slot: Int): ItemStack

    operator fun get(slot: Int): ItemStack = getItemStack(slot)

    fun transact(action: (setter: (slot: Int, itemStack: ItemStack?) -> Unit) -> Unit)

    fun getViewableInventory(): BlockInventory

    fun readTags(): TagReader
}
