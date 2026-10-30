package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.inventory.InventoryButtonClickEvent
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryProperty
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.MaterialTags
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.common.blockinventory.PlayerInventoryStash
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import java.util.concurrent.ConcurrentHashMap

private val LECTERN_BOOK_TAG = Tag.NBT("Book")
private val LECTERN_PAGE_TAG = Tag.Integer("Page")

private val lecternBooks = MaterialTags.LECTERN_BOOKS

private const val PREVIOUS_PAGE_BUTTON = 1
private const val NEXT_PAGE_BUTTON = 2
private const val TAKE_BOOK_BUTTON = 3
private const val PAGE_JUMP_BUTTONS = 100

class LecternHandler : BlockHandler {
    private val menus = ConcurrentHashMap<MenuKey, LecternMenu>()

    override fun getKey(): Key = Key.key("minecraft:lectern")

    override fun getBlockEntityTags(): Collection<Tag<*>> = listOf(LECTERN_BOOK_TAG, LECTERN_PAGE_TAG)

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val block = interaction.block
        val book = readBook(block)
        val item = interaction.player.getItemInHand(interaction.hand)

        if (book == null) {
            if (!isLecternBook(item)) return true
            placeBook(interaction, item)
            return false
        }

        if (interaction.player.isSneaking && item.isAir) {
            takeBook(interaction.instance, interaction.blockPosition, interaction.player)
            return false
        }

        openMenu(interaction.instance, interaction.blockPosition, interaction.player, book, readPage(block))
        return false
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        if (destroy.newBlock.key() == destroy.block.key()) return

        closeMenu(destroy.instance, destroy.blockPosition)
        readBook(destroy.block)?.let {
            DroppedItemFactory.maybeDrop(destroy.instance, destroy.blockPosition, it)
        }
    }

    private fun placeBook(
        interaction: BlockHandler.Interaction,
        item: ItemStack,
    ) {
        val block =
            interaction.block
                .withTag(LECTERN_BOOK_TAG, encodeBook(item))
                .withTag(LECTERN_PAGE_TAG, 0)
                .withProperty("has_book", "true")
        interaction.instance.setBlock(interaction.blockPosition, block)
        consumeItem(interaction.player, interaction.hand, item)
    }

    private fun takeBook(
        instance: Instance,
        position: Point,
        player: Player,
    ) {
        val block = instance.getBlock(position)
        val book = readBook(block) ?: return

        closeMenu(instance, position)
        instance.setBlock(
            position,
            block
                .withTag(LECTERN_BOOK_TAG, null)
                .withTag(LECTERN_PAGE_TAG, null)
                .withProperty("has_book", "false"),
            false,
        )
        PlayerInventoryStash.add(player, book)
    }

    private fun openMenu(
        instance: Instance,
        position: Point,
        player: Player,
        book: ItemStack,
        page: Int,
    ) {
        val menu = menus.computeIfAbsent(MenuKey(instance, position)) { createMenu(instance, position, book) }
        menu.setBook(book)
        menu.setPage(page)
        player.openInventory(menu)
    }

    private fun createMenu(
        instance: Instance,
        position: Point,
        book: ItemStack,
    ): LecternMenu = LecternMenu(instance, position, pageCountOf(book))

    private fun closeMenu(
        instance: Instance,
        position: Point,
    ) {
        menus.remove(MenuKey(instance, position))?.let { menu ->
            menu.viewers.toList().forEach { it.closeInventory() }
        }
    }

    private fun readBook(block: Block): ItemStack? =
        block
            .getTag(LECTERN_BOOK_TAG)
            ?.let { ItemStack.CODEC.decode(Transcoder.NBT, it).orElse(null) }
            ?.takeUnless { it.isAir }

    private fun encodeBook(book: ItemStack): CompoundBinaryTag =
        ItemStack.CODEC.encode(Transcoder.NBT, book).orElseThrow() as CompoundBinaryTag

    private fun readPage(block: Block): Int = block.getTag(LECTERN_PAGE_TAG) ?: 0

    private fun pageCountOf(book: ItemStack): Int {
        val written = book.get(DataComponents.WRITTEN_BOOK_CONTENT)
        val writable = book.get(DataComponents.WRITABLE_BOOK_CONTENT)
        return (written?.pages()?.size ?: writable?.pages()?.size ?: 1).coerceAtLeast(1)
    }

    private fun isLecternBook(item: ItemStack): Boolean =
        item.material().let { material ->
            Material.staticRegistry().getTag(lecternBooks)?.contains(material) == true
        }

    private fun consumeItem(
        player: Player,
        hand: PlayerHand,
        item: ItemStack,
    ) {
        if (player.gameMode == GameMode.CREATIVE) return
        player.setItemInHand(hand, if (item.amount() <= 1) ItemStack.AIR else item.withAmount(item.amount() - 1))
    }

    private data class MenuKey(
        val instance: Instance,
        val position: Point,
    )

    private inner class LecternMenu(
        private val instance: Instance,
        private val position: Point,
        var pageCount: Int,
    ) : Inventory(InventoryType.LECTERN, Component.translatable("container.lectern")) {
        private var page = 0
        private var book = ItemStack.AIR

        init {
            eventNode().addListener(InventoryPreClickEvent::class.java) { event ->
                val click = event.click
                val touchesBookSlot =
                    if (click is Click.Drag) {
                        click.slots().any { it in 0 until size }
                    } else {
                        event.slot in 0 until size
                    }
                if (touchesBookSlot) event.isCancelled = true
            }
            eventNode().addListener(InventoryItemChangeEvent::class.java) { event ->
                if (event.slot == 0 && event.newItem != book) {
                    val displaced = event.newItem
                    setItemStack(0, book)
                    if (!displaced.isAir) {
                        viewers.firstOrNull()?.let { PlayerInventoryStash.add(it, displaced) }
                    }
                }
            }
            eventNode().addListener(InventoryButtonClickEvent::class.java) { event ->
                handleButton(event.player, event.buttonId)
            }
            eventNode().addListener(InventoryCloseEvent::class.java) {
                if (viewers.size <= 1) menus.remove(MenuKey(instance, position))
            }
        }

        fun setBook(book: ItemStack) {
            if (this.book == book) return
            this.book = book
            pageCount = pageCountOf(book)
            setItemStack(0, book)
        }

        fun setPage(page: Int) {
            val clamped = page.coerceIn(0, pageCount - 1)
            if (this.page == clamped) {
                sendPage()
                return
            }
            this.page = clamped
            sendPage()
            persistPage()
        }

        override fun addViewer(player: Player): Boolean {
            val added = super.addViewer(player)
            if (added) sendPage()
            return added
        }

        private fun handleButton(
            player: Player,
            buttonId: Int,
        ) {
            when (buttonId) {
                PREVIOUS_PAGE_BUTTON -> setPage(page - 1)
                NEXT_PAGE_BUTTON -> setPage(page + 1)
                TAKE_BOOK_BUTTON -> takeBook(instance, position, player)
                in PAGE_JUMP_BUTTONS..(PAGE_JUMP_BUTTONS + pageCount) -> setPage(buttonId - PAGE_JUMP_BUTTONS)
            }
        }

        private fun sendPage() {
            sendProperty(InventoryProperty.LECTERN_PAGE_NUMBER, page.toShort())
        }

        private fun persistPage() {
            val block = instance.getBlock(position)
            if (readBook(block) == null) return
            instance.setBlock(position, block.withTag(LECTERN_PAGE_TAG, page), false)
        }
    }
}
