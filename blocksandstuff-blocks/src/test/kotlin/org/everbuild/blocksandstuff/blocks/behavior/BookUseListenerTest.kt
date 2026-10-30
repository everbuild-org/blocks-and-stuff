package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.book.EditBookEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.OpenBookPacket
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class BookUseListenerTest {
    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BookUseListener.enable()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun savingWritableBookStoresPages() {
        val book = ItemStack.of(Material.WRITABLE_BOOK)
        val updated = edit(book, listOf("Hello", "World"), null)

        Assertions.assertEquals(Material.WRITABLE_BOOK, updated.material())
        val content = requireNotNull(updated.get(DataComponents.WRITABLE_BOOK_CONTENT))
        Assertions.assertEquals(listOf("Hello", "World"), content.pages().map { it.text() })
    }

    @Test
    fun signingWritableBookCreatesWrittenBook() {
        val book = ItemStack.of(Material.WRITABLE_BOOK)
        val updated = edit(book, listOf("Hello", "World"), "My Book")

        Assertions.assertEquals(Material.WRITTEN_BOOK, updated.material())
        Assertions.assertNull(updated.get(DataComponents.WRITABLE_BOOK_CONTENT))
        val content = requireNotNull(updated.get(DataComponents.WRITTEN_BOOK_CONTENT))
        Assertions.assertEquals("My Book", content.title().text())
        Assertions.assertEquals("tester", content.author())
        Assertions.assertEquals(2, content.pages().size)
    }

    @Test
    fun writtenBookOpensReader() {
        val book = ItemStack.of(Material.WRITTEN_BOOK)
        val player =
            mock<Player> {
                on { getItemInHand(any()) } doReturn book
            }
        val event = PlayerUseItemEvent(player, PlayerHand.MAIN, book, 0)

        EventDispatcher.call(event)

        verify(player).sendPacket(OpenBookPacket(PlayerHand.MAIN))
    }

    @Test
    fun writableBookIsLeftToClientEditor() {
        val book = ItemStack.of(Material.WRITABLE_BOOK)
        val player =
            mock<Player> {
                on { getItemInHand(any()) } doReturn book
            }
        val event = PlayerUseItemEvent(player, PlayerHand.MAIN, book, 0)

        EventDispatcher.call(event)

        Assertions.assertFalse(event.isCancelled)
    }

    private fun edit(
        book: ItemStack,
        pages: List<String>,
        title: String?,
    ): ItemStack {
        var updated = book
        val player =
            mock<Player> {
                on { getItemInHand(any()) } doReturn book
                on { username } doReturn "tester"
                on { setItemInHand(any(), any()) } doAnswer {
                    updated = it.getArgument(1)
                    Unit
                }
            }

        EventDispatcher.call(EditBookEvent(player, book, pages, title))

        return updated
    }
}
