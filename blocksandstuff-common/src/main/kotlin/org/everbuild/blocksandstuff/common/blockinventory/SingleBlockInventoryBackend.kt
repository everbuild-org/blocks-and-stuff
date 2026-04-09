package org.everbuild.blocksandstuff.common.blockinventory

import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.codec.Transcoder
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag

open class SingleBlockInventoryBackend protected constructor(
    override val archetype: BlockInventoryArchetype,
    private val blockPos: Point,
    private val instance: Instance,
) : PhysicalInventory {
    val itemStacks = Array(archetype.size) { ItemStack.AIR }

    private var inventory: BlockInventory? = null

    init {
        load()
    }

    override fun transact(action: (setter: (slot: Int, itemStack: ItemStack?) -> Unit) -> Unit) {
        var hasChanged = false
        action { slot, itemStack ->
            val old = itemStacks[slot]
            val new = itemStack ?: ItemStack.AIR
            if (!old.equals(new)) {
                updateSlotUnsafe(slot, new)
                hasChanged = true
            }
        }
        if (hasChanged) save()
    }

    override fun getItemStack(slot: Int): ItemStack = itemStacks[slot]

    private fun updateSlotUnsafe(slot: Int, itemStack: ItemStack) {
        itemStacks[slot] = itemStack
        inventory?.updateStackRaw(slot, itemStack)
    }

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
        val data = (block ?: instance.getBlock(blockPos)).getTag(itemsTag)
            ?: return

        val slots = data.associateBy { (it as CompoundBinaryTag).getByte("Slot").toInt() }

        for (i in itemStacks.indices) {
            val itemNBT = slots[i]
            if (itemNBT == null) {
                itemStacks[i] = ItemStack.AIR
                continue
            }
            itemStacks[i] = ItemStack.CODEC.decode(Transcoder.NBT, itemNBT).orElseThrow()
        }
    }

    @Synchronized
    override fun getViewableInventory(): BlockInventory {
        inventory?.let { return it }
        return archetype.createInventory(this)
            .also { inventory = it }
    }

    override fun readTags(): TagReader {
        val atPos = instance.getBlock(blockPos)
        return TagReader.Readable(atPos)
    }

    companion object {
        private val itemsTag = Tag.NBT("Items").list()
    }
}