package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.book.EditBookEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.book.FilteredText
import net.minestom.server.item.component.WritableBookContent
import net.minestom.server.item.component.WrittenBookContent
import net.minestom.server.network.packet.server.play.OpenBookPacket

object BookUseListener {
    private var registeredHandler: Any? = null

    fun enable() {
        val eventHandler = MinecraftServer.getGlobalEventHandler()
        if (registeredHandler === eventHandler) return
        registeredHandler = eventHandler

        eventHandler.addListener(PlayerUseItemEvent::class.java) { event ->
            if (event.itemStack.material() != Material.WRITTEN_BOOK) return@addListener

            event.player.sendPacket(OpenBookPacket(event.hand))
        }

        eventHandler.addListener(PlayerUseItemOnBlockEvent::class.java) { event ->
            if (event.itemStack.material() != Material.WRITTEN_BOOK) return@addListener

            event.player.sendPacket(OpenBookPacket(event.hand))
        }

        eventHandler.addListener(EditBookEvent::class.java) { event ->
            val original = event.itemStack
            if (original.material() != Material.WRITABLE_BOOK) return@addListener

            val title = event.title
            val updated =
                if (title != null) {
                    signBook(original, event.player, title, event.pages)
                } else {
                    writePages(original, event.pages)
                }
            replaceInHand(event.player, original, updated)
        }
    }

    private fun writePages(
        book: ItemStack,
        pages: List<String>,
    ): ItemStack {
        val content = WritableBookContent(pages.map { FilteredText(it, null) })
        return book.with(DataComponents.WRITABLE_BOOK_CONTENT, content)
    }

    private fun signBook(
        book: ItemStack,
        player: Player,
        title: String,
        pages: List<String>,
    ): ItemStack {
        val content =
            WrittenBookContent(
                title,
                player.username,
                pages.map { Component.text(it) },
            )
        return book
            .builder()
            .material(Material.WRITTEN_BOOK)
            .remove(DataComponents.WRITABLE_BOOK_CONTENT)
            .set(DataComponents.WRITTEN_BOOK_CONTENT, content)
            .build()
    }

    private fun replaceInHand(
        player: Player,
        original: ItemStack,
        updated: ItemStack,
    ) {
        when {
            player.getItemInHand(PlayerHand.MAIN) == original -> player.setItemInHand(PlayerHand.MAIN, updated)
            player.getItemInHand(PlayerHand.OFF) == original -> player.setItemInHand(PlayerHand.OFF, updated)
        }
    }
}
