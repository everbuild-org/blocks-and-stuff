package org.everbuild.blocksandstuff.blocks.util

import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponents
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.component.TypedCustomData
import net.minestom.server.tag.Tag

private const val SHULKER_BOX_ENTITY = "minecraft:shulker_box"
private val ITEMS_TAG = Tag.NBT("Items").list()

internal fun Block.toItemWithBlockEntityData(): ItemStack? {
    val material = material() ?: return null
    val item = ItemStack.of(material)
    val blockEntityType = blockEntityType() ?: return item
    val nbt = nbt() ?: return item
    return item.with(DataComponents.BLOCK_ENTITY_DATA, TypedCustomData(blockEntityType, nbt))
}

internal fun Block.applyItemBlockEntityData(itemStack: ItemStack?): Block {
    val data = itemStack?.get(DataComponents.BLOCK_ENTITY_DATA) ?: return this
    if (data.type() !== blockEntityType()) return this
    return withNbt(data.nbt())
}

internal fun Block.applyItemContainerContents(itemStack: ItemStack?): Block {
    val contents = itemStack?.get(DataComponents.CONTAINER) ?: return this
    if (blockEntityType() == null) return this

    val items =
        contents.mapIndexedNotNull { slot, stack ->
            stack
                .takeUnless { it.isAir }
                ?.let { encodeItem(it, slot) }
        }
    return withTag(ITEMS_TAG, items)
}

internal fun Block.isShulkerBox(): Boolean = blockEntityType()?.key()?.asString() == SHULKER_BOX_ENTITY

/**
 * Item form of a container block that keeps its contents. Shulker boxes store the stacks in the
 * `container` component and additionally get a lore preview.
 */
internal fun Block.toContainerItem(): ItemStack? {
    val material = material() ?: return null
    if (!isShulkerBox()) return toItemWithBlockEntityData()

    val contents = decodeContainerItems(nbt())
    val item = ItemStack.of(material)
    if (contents.isEmpty()) return item
    return item
        .with(DataComponents.CONTAINER, contents)
        .with(DataComponents.LORE, contents.map(::containerLoreLine))
}

private fun decodeContainerItems(nbt: CompoundBinaryTag?): List<ItemStack> {
    val listed = nbt?.getList("Items") ?: return emptyList()
    return listed
        .asSequence()
        .filterIsInstance<CompoundBinaryTag>()
        .map { tag -> tag.getByte("Slot").toInt() to ItemStack.CODEC.decode(Transcoder.NBT, tag).orElseThrow() }
        .filter { (_, stack) -> !stack.isAir }
        .sortedBy { (slot, _) -> slot }
        .map { (_, stack) -> stack }
        .toList()
}

private fun encodeItem(
    stack: ItemStack,
    slot: Int,
): CompoundBinaryTag =
    (ItemStack.CODEC.encode(Transcoder.NBT, stack).orElseThrow() as CompoundBinaryTag)
        .putByte("Slot", slot.toByte())

private fun containerLoreLine(stack: ItemStack): Component {
    val name =
        stack.get(DataComponents.CUSTOM_NAME)
            ?: Component.translatable(stack.material().translationKey(), stack.material().key().value())
    return Component.text("${stack.amount()}x ").append(name)
}
