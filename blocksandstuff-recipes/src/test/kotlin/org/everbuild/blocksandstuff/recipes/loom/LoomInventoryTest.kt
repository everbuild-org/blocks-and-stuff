package org.everbuild.blocksandstuff.recipes.loom

import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryButtonClickEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.MaterialTags
import net.minestom.server.network.NetworkBuffer
import org.everbuild.blocksandstuff.recipes.util.isIn
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoomInventoryTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            MinecraftServer.init()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            MinecraftServer.stopCleanly()
        }
    }

    @Test
    fun `loom material tags resolve`() {
        assert(Material.WHITE_BANNER.isIn(MaterialTags.BANNERS)) { "banners tag empty" }
        assert(Material.RED_DYE.isIn(MaterialTags.LOOM_DYES)) { "loom_dyes tag empty" }
        assert(Material.CREEPER_BANNER_PATTERN.isIn(MaterialTags.LOOM_PATTERNS)) { "loom_patterns tag empty" }
    }

    @Test
    fun `produces patterned banner from banner dye and pattern`() {
        val loom = LoomInventory()
        loom.setItemStack(LoomInventory.BANNER_SLOT, ItemStack.of(Material.WHITE_BANNER))
        loom.setItemStack(LoomInventory.DYE_SLOT, ItemStack.of(Material.RED_DYE))
        loom.setItemStack(LoomInventory.PATTERN_SLOT, ItemStack.of(Material.CREEPER_BANNER_PATTERN))

        val result = loom.getItemStack(LoomInventory.RESULT_SLOT)
        assertFalse(result.isAir, "loom did not compute a result")
        assertEquals(Material.WHITE_BANNER, result.material())
        assertEquals(1, result.get(DataComponents.BANNER_PATTERNS)?.layers()?.size)
    }

    @Test
    fun `banner and dye with a selected pattern produce a result`() {
        val loom = LoomInventory()
        loom.setItemStack(LoomInventory.BANNER_SLOT, ItemStack.of(Material.WHITE_BANNER))
        loom.setItemStack(LoomInventory.DYE_SLOT, ItemStack.of(Material.RED_DYE))

        EventDispatcher.call(InventoryButtonClickEvent(mock<Player>(), loom, 0))

        val result = loom.getItemStack(LoomInventory.RESULT_SLOT)
        assertFalse(result.isAir, "loom did not produce a result from banner + dye + selected pattern")
        assertEquals(1, result.amount())
        assertEquals(1, result.get(DataComponents.BANNER_PATTERNS)?.layers()?.size)
    }

    @Test
    fun `result item survives a network round trip`() {
        val loom = LoomInventory()
        loom.setItemStack(LoomInventory.BANNER_SLOT, ItemStack.of(Material.WHITE_BANNER))
        loom.setItemStack(LoomInventory.DYE_SLOT, ItemStack.of(Material.RED_DYE))
        loom.setItemStack(LoomInventory.PATTERN_SLOT, ItemStack.of(Material.CREEPER_BANNER_PATTERN))

        val result = loom.getItemStack(LoomInventory.RESULT_SLOT)
        val buffer = NetworkBuffer.resizableBuffer(MinecraftServer.process())
        buffer.write(ItemStack.NETWORK_TYPE, result)
        val decoded = buffer.read(ItemStack.NETWORK_TYPE)
        assertEquals(Material.WHITE_BANNER, decoded.material())
        assertEquals(1, decoded.get(DataComponents.BANNER_PATTERNS)?.layers()?.size)
    }

    @Test
    fun `taking result puts banner on cursor and consumes inputs`() {
        val loom = LoomInventory()
        loom.setItemStack(LoomInventory.BANNER_SLOT, ItemStack.of(Material.WHITE_BANNER))
        loom.setItemStack(LoomInventory.DYE_SLOT, ItemStack.of(Material.RED_DYE))
        loom.setItemStack(LoomInventory.PATTERN_SLOT, ItemStack.of(Material.CREEPER_BANNER_PATTERN))

        val playerInventory = mock<PlayerInventory>()
        whenever(playerInventory.cursorItem).thenReturn(ItemStack.AIR)
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        EventDispatcher.call(InventoryPreClickEvent(loom, player, Click.Left(LoomInventory.RESULT_SLOT)))

        val cursor = argumentCaptor<ItemStack>()
        verify(playerInventory).cursorItem = cursor.capture()
        assertEquals(Material.WHITE_BANNER, cursor.firstValue.material())
        assertEquals(1, cursor.firstValue.amount())
        assertFalse(cursor.firstValue.isAir)
    }

    @Test
    fun `shift click crafts the whole stack and consumes the matching amount`() {
        val loom = LoomInventory()
        loom.setItemStack(LoomInventory.BANNER_SLOT, ItemStack.of(Material.WHITE_BANNER, 3))
        loom.setItemStack(LoomInventory.DYE_SLOT, ItemStack.of(Material.RED_DYE, 5))
        loom.setItemStack(LoomInventory.PATTERN_SLOT, ItemStack.of(Material.CREEPER_BANNER_PATTERN))

        val playerInventory = PlayerInventory()
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        EventDispatcher.call(InventoryPreClickEvent(loom, player, Click.LeftShift(LoomInventory.RESULT_SLOT)))

        val banners = playerInventory.itemStacks.filter { it.material() == Material.WHITE_BANNER }.sumOf { it.amount() }
        assertEquals(3, banners, "min(3 banner, 5 dye) banners must be crafted")
        assertTrue(loom.getItemStack(LoomInventory.BANNER_SLOT).isAir)
        assertEquals(2, loom.getItemStack(LoomInventory.DYE_SLOT).amount())
    }
}
