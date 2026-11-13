package org.everbuild.blocksandstuff.blocks.util

import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.codec.Transcoder
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.common.util.itemNbtTranscoder

private val ITEMS_TAG: Tag<List<net.kyori.adventure.nbt.BinaryTag>> = Tag.NBT("Items").list()

/** Reads the vanilla `Items` block-entity list as slot -> item (empty stacks omitted). */
internal fun Block.readBlockEntityItems(): Map<Int, ItemStack> {
    val listed = getTag(ITEMS_TAG) ?: return emptyMap()
    return listed
        .asSequence()
        .filterIsInstance<CompoundBinaryTag>()
        .filterNot { it.isAirItem() }
        .mapNotNull { tag ->
            val slot = tag.getByte("Slot").toInt()
            val stack = runCatching { ItemStack.CODEC.decode(itemNbtTranscoder(), tag).orElseThrow() }.getOrNull() ?: return@mapNotNull null
            if (stack.isAir) null else slot to stack
        }.toMap()
}

/** Writes the vanilla `Items` block-entity list, preserving slot indices. */
internal fun Block.withBlockEntityItems(items: Map<Int, ItemStack>): Block {
    val list =
        items
            .filterValues { !it.isAir }
            .map { (slot, stack) ->
                (ItemStack.CODEC.encode(itemNbtTranscoder(), stack).orElseThrow() as CompoundBinaryTag)
                    .putByte("Slot", slot.toByte())
            }
    return withTag(ITEMS_TAG, list)
}
