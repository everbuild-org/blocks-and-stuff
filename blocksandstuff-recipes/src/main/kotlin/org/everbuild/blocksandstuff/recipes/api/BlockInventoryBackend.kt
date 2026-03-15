package org.everbuild.blocksandstuff.recipes.api

import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.codec.Transcoder
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag

open class BlockInventoryBackend(
    val size: Int,
    private val blockPos: Point,
    private val instance: Instance
) {
    val itemStacks = Array(size) { ItemStack.AIR }
    private val inventories = mutableListOf<BlockInventory>()

    init {
        load()
    }

    fun getItemStack(slot: Int): ItemStack = itemStacks[slot]
    fun setItemStack(slot: Int, itemStack: ItemStack?) {
        itemStacks[slot] = itemStack ?: ItemStack.AIR
        inventories.forEach {
            it.updateStackRaw(slot, itemStacks[slot])
        }
    }

    operator fun get(slot: Int): ItemStack = getItemStack(slot)
    operator fun set(slot: Int, itemStack: ItemStack?) = setItemStack(slot, itemStack)

    fun save() {
        val itemsNBT = this.itemStacks
            .mapIndexed { index, itemStack -> index to itemStack }
            .filter { (_, stack) -> stack != ItemStack.AIR }
            .map { (i, stack) -> i.toByte() to ItemStack.CODEC.encode(Transcoder.NBT, stack).orElseThrow() }
            .map { (slot, itemNBT) -> (itemNBT as CompoundBinaryTag).putByte("Slot", slot) }
            .toList()
        instance.setBlock(blockPos, instance.getBlock(blockPos).withTag(itemsTag, itemsNBT), false)
    }

    fun load(block: Block? = null) {
        val data = instance.getBlock(blockPos).getTag(itemsTag)
        if (data != null) {
            for (itemNBT in data) {
                val slot = (itemNBT as CompoundBinaryTag).getByte("Slot").toInt()
                this.setItemStack(slot, ItemStack.CODEC.decode(Transcoder.NBT, itemNBT).orElseThrow())
            }
        }
    }

    fun attach(inventory: BlockInventory) {
        inventories.add(inventory)

        itemStacks.forEachIndexed { index, itemStack ->
            if (itemStack != ItemStack.AIR) {
                inventory.updateStackRaw(index, itemStack)
            }
        }
    }

    fun detach(inventory: BlockInventory) {
        inventories.remove(inventory)
    }

    companion object {
        private val itemsTag = Tag.NBT("Items").list()
    }
}