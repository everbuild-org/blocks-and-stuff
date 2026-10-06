package org.everbuild.blocksandstuff.recipes.grid

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.RecipeBookCategory
import org.everbuild.blocksandstuff.recipes.ingredient
import org.everbuild.blocksandstuff.recipes.ingredientGrid
import org.everbuild.blocksandstuff.recipes.initRecipeServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CraftingTableInventoryTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = initRecipeServer()
    }

    @Test
    fun `crafting table computes the result and hands it to the cursor`() {
        val planks = ingredient(Material.OAK_PLANKS)
        MinecraftServer.getRecipeManager().addRecipe(
            ShapedCraftingRecipe(
                ingredientGrid(listOf(planks, planks), listOf(planks, planks)),
                ItemStack.of(Material.CRAFTING_TABLE),
                null,
                RecipeBookCategory.CRAFTING_MISC,
            ),
        )

        val inventory = CraftingTableInventory()
        inventory.setItemStack(1, ItemStack.of(Material.OAK_PLANKS))
        inventory.setItemStack(2, ItemStack.of(Material.OAK_PLANKS))
        inventory.setItemStack(4, ItemStack.of(Material.OAK_PLANKS))
        inventory.setItemStack(5, ItemStack.of(Material.OAK_PLANKS))

        assertEquals(Material.CRAFTING_TABLE, inventory.getItemStack(0).material())

        val playerInventory = mock<PlayerInventory>()
        whenever(playerInventory.cursorItem).thenReturn(ItemStack.AIR)
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        EventDispatcher.call(InventoryPreClickEvent(inventory, player, Click.Left(0)))

        val cursor = argumentCaptor<ItemStack>()
        verify(playerInventory).cursorItem = cursor.capture()
        assertEquals(Material.CRAFTING_TABLE, cursor.firstValue.material())

        assertTrue(inventory.getItemStack(1).isAir)
        assertTrue(inventory.getItemStack(2).isAir)
        assertTrue(inventory.getItemStack(4).isAir)
        assertTrue(inventory.getItemStack(5).isAir)
    }
}
