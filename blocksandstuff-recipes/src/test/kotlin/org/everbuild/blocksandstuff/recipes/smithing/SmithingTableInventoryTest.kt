package org.everbuild.blocksandstuff.recipes.smithing

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.recipes.RecipeFactory
import org.everbuild.blocksandstuff.recipes.ingredient
import org.everbuild.blocksandstuff.recipes.initRecipeServer
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SmithingTableInventoryTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = initRecipeServer()
    }

    @Test
    fun `smithing transforms the base and consumes the inputs`() {
        MinecraftServer.getRecipeManager().addRecipe(
            TransformSmithingRecipe(
                ingredient(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                ingredient(Material.DIAMOND_SWORD),
                ingredient(Material.NETHERITE_INGOT),
                RecipeIngredient.MaterialItem(Material.NETHERITE_SWORD.key(), RecipeFactory.itemController),
            ),
        )

        val inventory = SmithingTableInventory()
        inventory.setItemStack(SmithingTableInventory.TEMPLATE_SLOT, ItemStack.of(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE))
        inventory.setItemStack(SmithingTableInventory.BASE_SLOT, ItemStack.of(Material.DIAMOND_SWORD))
        inventory.setItemStack(SmithingTableInventory.ADDITION_SLOT, ItemStack.of(Material.NETHERITE_INGOT))

        assertEquals(Material.NETHERITE_SWORD, inventory.getItemStack(SmithingTableInventory.RESULT_SLOT).material())

        val playerInventory = mock<PlayerInventory>()
        whenever(playerInventory.cursorItem).thenReturn(ItemStack.AIR)
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        EventDispatcher.call(InventoryPreClickEvent(inventory, player, Click.Left(SmithingTableInventory.RESULT_SLOT)))

        val cursor = argumentCaptor<ItemStack>()
        verify(playerInventory).cursorItem = cursor.capture()
        assertEquals(Material.NETHERITE_SWORD, cursor.firstValue.material())

        assertTrue(inventory.getItemStack(SmithingTableInventory.TEMPLATE_SLOT).isAir)
        assertTrue(inventory.getItemStack(SmithingTableInventory.BASE_SLOT).isAir)
        assertTrue(inventory.getItemStack(SmithingTableInventory.ADDITION_SLOT).isAir)
    }
}
