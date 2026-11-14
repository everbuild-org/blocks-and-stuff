package org.everbuild.blocksandstuff.blocks.util

import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.minestom.server.component.DataComponents
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.component.TypedCustomData
import org.everbuild.blocksandstuff.common.util.itemNbtTranscoder
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
    val item = toItemWithBlockEntityData() ?: return null
    if (!isShulkerBox()) return item

    val contents = decodeContainerItems(nbt())
    if (contents.isEmpty()) return item
    return item.with(DataComponents.LORE, contents.map(::containerLoreLine))
}

private fun decodeContainerItems(nbt: CompoundBinaryTag?): List<ItemStack> {
    val listed = nbt?.getList("Items") ?: return emptyList()
    return listed
        .asSequence()
        .filterIsInstance<CompoundBinaryTag>()
        .filterNot { it.isAirItem() }
        .map { tag -> tag.getByte("Slot").toInt() to ItemStack.CODEC.decode(itemNbtTranscoder(), tag).orElseThrow() }
        .filter { (_, stack) -> !stack.isAir }
        .sortedBy { (slot, _) -> slot }
        .map { (_, stack) -> stack }
        .toList()
}

internal fun CompoundBinaryTag.isAirItem(): Boolean {
    if (getString("id") == "minecraft:air") return true
    return contains("count") && getInt("count") <= 0
}

private fun encodeItem(
    stack: ItemStack,
    slot: Int,
): CompoundBinaryTag =
    (ItemStack.CODEC.encode(itemNbtTranscoder(), stack).orElseThrow() as CompoundBinaryTag)
        .putByte("Slot", slot.toByte())

private fun containerLoreLine(stack: ItemStack): Component {
    val name =
        stack.get(DataComponents.CUSTOM_NAME)
            ?: Component.translatable(stack.material().translationKey(), stack.material().key().value())
    return Component.text("${stack.amount()}x ").append(name)
}
