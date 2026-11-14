package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class LecternBehaviourTest {
    private lateinit var instance: Instance
    private lateinit var handler: LecternHandler
    private val position = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        handler = LecternHandler()
        instance.setBlock(position, Block.LECTERN.withHandler(handler))
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun interact(
        item: ItemStack,
        sneaking: Boolean = false,
    ): Pair<Player, Boolean> {
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.SURVIVAL
                on { isSneaking } doReturn sneaking
                on { getItemInHand(any()) } doReturn item
                on { openInventory(any()) } doReturn true
            }
        val block = instance.getBlock(position)
        val handled =
            handler.onInteract(
                BlockHandler.Interaction(block, instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN),
            )
        return player to handled
    }

    @Test
    fun placesBookAndMarksBlock() {
        val (_, handled) = interact(ItemStack.of(Material.WRITTEN_BOOK))

        Assertions.assertFalse(handled)
        val block = instance.getBlock(position)
        Assertions.assertEquals("true", block.getProperty("has_book"))
        Assertions.assertNotNull(block.nbt())
        Assertions.assertEquals(
            0,
            block.getTag(
                net.minestom.server.tag.Tag
                    .Integer("Page"),
            ),
        )
    }

    @Test
    fun opensLecternMenuWithBook() {
        interact(ItemStack.of(Material.WRITTEN_BOOK))

        val (player, handled) = interact(ItemStack.AIR)

        Assertions.assertFalse(handled)
        verify(player).openInventory(any<Inventory>())
        Assertions.assertEquals("true", instance.getBlock(position).getProperty("has_book"))
    }

    @Test
    fun exposesBlockEntityTags() {
        val keys = handler.getBlockEntityTags().map { it.key }.toSet()

        Assertions.assertTrue(keys.contains("Book"))
        Assertions.assertTrue(keys.contains("Page"))
    }
}
