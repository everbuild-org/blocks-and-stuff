package org.everbuild.blocksandstuff.recipes.stonecutting

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryButtonClickEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.recipes.ingredient
import org.everbuild.blocksandstuff.recipes.initRecipeServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class StonecutterInventoryTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = initRecipeServer()
    }

    @Test
    fun `button selects the recipe in client registration order`() {
        // Registration order is slab then stairs. The old material-name sort would return
        // stone_stairs for button 0, so this guards the ordering fix.
        val input = ingredient(Material.STONE)
        MinecraftServer.getRecipeManager().addRecipe(StonecutterRecipe(input, ItemStack.of(Material.STONE_SLAB), null))
        MinecraftServer.getRecipeManager().addRecipe(StonecutterRecipe(input, ItemStack.of(Material.STONE_STAIRS), null))

        val inventory = StonecutterInventory()
        inventory.setItemStack(inventory.inputSlot, ItemStack.of(Material.STONE))

        EventDispatcher.call(InventoryButtonClickEvent(mock<Player>(), inventory, 0))
        assertEquals(Material.STONE_SLAB, inventory.getItemStack(inventory.outputSlot).material())

        EventDispatcher.call(InventoryButtonClickEvent(mock<Player>(), inventory, 1))
        assertEquals(Material.STONE_STAIRS, inventory.getItemStack(inventory.outputSlot).material())
    }

    @Test
    fun `shift click crafts the whole input and respects the result amount`() {
        val input = ingredient(Material.COBBLESTONE)
        // 1 cobblestone -> 2 slabs (like vanilla stonecutter recipes that yield more than one item)
        MinecraftServer.getRecipeManager().addRecipe(StonecutterRecipe(input, ItemStack.of(Material.COBBLESTONE_SLAB, 2), null))

        val inventory = StonecutterInventory()
        inventory.setItemStack(inventory.inputSlot, ItemStack.of(Material.COBBLESTONE, 3))

        val playerInventory = PlayerInventory()
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        EventDispatcher.call(InventoryButtonClickEvent(player, inventory, 0))
        EventDispatcher.call(InventoryPreClickEvent(inventory, player, Click.LeftShift(inventory.outputSlot)))

        val slabs = playerInventory.itemStacks.filter { it.material() == Material.COBBLESTONE_SLAB }.sumOf { it.amount() }
        assertEquals(6, slabs, "3 cobblestone * 2 slabs must be given")
        assertTrue(inventory.getItemStack(inventory.inputSlot).isAir)
    }

    @Test
    fun `shift click moves a stack into the input slot`() {
        val inventory = StonecutterInventory()
        val playerInventory = PlayerInventory()
        playerInventory.setItemStack(0, ItemStack.of(Material.STONE, 4))
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        assertTrue(inventory.shiftClick(player, inventory.size + 0, 0))
        assertEquals(4, inventory.getItemStack(inventory.inputSlot).amount())
        assertTrue(playerInventory.getItemStack(0).isAir)
    }

    @Test
    fun `shift click never routes items into the output slot`() {
        val inventory = StonecutterInventory()
        inventory.setItemStack(inventory.inputSlot, ItemStack.of(Material.STONE, 64))

        val playerInventory = PlayerInventory()
        playerInventory.setItemStack(3, ItemStack.of(Material.COBBLESTONE))
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        inventory.shiftClick(player, inventory.size + 3, 0)

        assertTrue(inventory.getItemStack(inventory.outputSlot).isAir, "input must never land in the result slot")
        assertEquals(Material.COBBLESTONE, playerInventory.getItemStack(3).material())
    }

    @Test
    fun `repeated clicks consume one input and give the recipe amount each time`() {
        val input = ingredient(Material.DEEPSLATE)
        MinecraftServer.getRecipeManager().addRecipe(StonecutterRecipe(input, ItemStack.of(Material.DEEPSLATE_TILE_SLAB, 2), null))

        val inventory = StonecutterInventory()
        inventory.setItemStack(inventory.inputSlot, ItemStack.of(Material.DEEPSLATE, 2))

        val playerInventory = PlayerInventory()
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        EventDispatcher.call(InventoryButtonClickEvent(player, inventory, 0))
        EventDispatcher.call(InventoryPreClickEvent(inventory, player, Click.Left(inventory.outputSlot)))
        assertEquals(2, playerInventory.cursorItem.amount())
        assertEquals(1, inventory.getItemStack(inventory.inputSlot).amount())

        EventDispatcher.call(InventoryPreClickEvent(inventory, player, Click.Left(inventory.outputSlot)))
        assertEquals(4, playerInventory.cursorItem.amount(), "second click must add exactly one craft's output")
        assertTrue(inventory.getItemStack(inventory.inputSlot).isAir)
    }
}
