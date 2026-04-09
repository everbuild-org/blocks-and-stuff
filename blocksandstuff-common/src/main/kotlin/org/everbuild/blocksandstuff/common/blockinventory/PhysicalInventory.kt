package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.item.ItemStack

interface PhysicalInventory {
    val archetype: BlockInventoryArchetype

    fun getItemStack(slot: Int): ItemStack
    operator fun get(slot: Int): ItemStack = getItemStack(slot)

    fun transact(action: (setter: (slot: Int, itemStack: ItemStack?) -> Unit) -> Unit)

    fun getViewableInventory(): BlockInventory

    fun readTags(): TagReader
}