package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack

interface PhysicalInventory {
    val archetype: BlockInventoryArchetype

    /** Slots that may receive items from other inventories (default: all slots). */
    val insertableSlots: Collection<Int> get() = List(archetype.size) { it }

    /**
     * Slots a neighbouring inventory may insert into through [face] (the face of this inventory
     * the item enters through). Defaults to [insertableSlots].
     */
    fun insertableSlotsFor(face: BlockFace): Collection<Int> = insertableSlots

    /** Whether [stack] may be placed into [slot]. */
    fun canPlaceItem(
        slot: Int,
        stack: ItemStack,
    ): Boolean = true

    /** Called when an item was rejected by [canPlaceItem] so the implementation can return it. */
    fun onItemRejected(stack: ItemStack) {}

    /** Slots that may be drained by other inventories (default: all slots). */
    val extractableSlots: Collection<Int> get() = List(archetype.size) { it }

    fun getItemStack(slot: Int): ItemStack

    operator fun get(slot: Int): ItemStack = getItemStack(slot)

    fun transact(action: (setter: (slot: Int, itemStack: ItemStack?) -> Unit) -> Unit)

    fun getViewableInventory(): BlockInventory

    fun readTags(): TagReader
}
