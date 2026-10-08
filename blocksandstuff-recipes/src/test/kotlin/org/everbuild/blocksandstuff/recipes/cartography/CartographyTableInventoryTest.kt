package org.everbuild.blocksandstuff.recipes.cartography

import net.minestom.server.entity.Player
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.recipes.initRecipeServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CartographyTableInventoryTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = initRecipeServer()
    }

    @Test
    fun `shift click routes a map into the map slot`() {
        val inventory = CartographyTableInventory()
        val playerInventory = mock<PlayerInventory>()
        val clickSlot = 5
        whenever(playerInventory.getItemStack(clickSlot)).thenReturn(ItemStack.of(Material.FILLED_MAP))
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        assertTrue(inventory.shiftClick(player, inventory.size + clickSlot, 0))
        assertEquals(Material.FILLED_MAP, inventory.getItemStack(CartographyTableInventory.MAP_SLOT).material())
    }

    @Test
    fun `shift click routes paper into the additional slot`() {
        val inventory = CartographyTableInventory()
        val playerInventory = mock<PlayerInventory>()
        val clickSlot = 7
        whenever(playerInventory.getItemStack(clickSlot)).thenReturn(ItemStack.of(Material.PAPER))
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        assertTrue(inventory.shiftClick(player, inventory.size + clickSlot, 0))
        assertEquals(Material.PAPER, inventory.getItemStack(CartographyTableInventory.ADDITIONAL_SLOT).material())
    }

    @Test
    fun `shift click ignores items that do not belong in the table`() {
        val inventory = CartographyTableInventory()
        val playerInventory = mock<PlayerInventory>()
        val clickSlot = 3
        whenever(playerInventory.getItemStack(clickSlot)).thenReturn(ItemStack.of(Material.DIRT))
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        assertFalse(inventory.shiftClick(player, inventory.size + clickSlot, 0))
    }
}
