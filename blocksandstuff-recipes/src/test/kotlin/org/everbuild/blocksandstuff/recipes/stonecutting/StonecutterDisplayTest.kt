package org.everbuild.blocksandstuff.recipes.stonecutting

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryButtonClickEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.ConnectionState
import net.minestom.server.network.packet.server.CachedPacket
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket
import net.minestom.server.recipe.display.SlotDisplay
import org.everbuild.blocksandstuff.recipes.RecipeFactory
import org.everbuild.blocksandstuff.recipes.ingredient
import org.everbuild.blocksandstuff.recipes.initRecipeServer
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredient
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class StonecutterDisplayTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = initRecipeServer()

        @Suppress("UNCHECKED_CAST")
        private fun stonecutterEntries(): List<DeclareRecipesPacket.StonecutterRecipe> {
            val cached = MinecraftServer.getRecipeManager().getDeclareRecipesPacket() as CachedPacket
            val packet = cached.packet(ConnectionState.PLAY) as DeclareRecipesPacket
            return packet.stonecutterRecipes()
        }
    }

    @Test
    fun `only item displays reach the client and button order stays aligned`() {
        // Minestom only forwards Item/Tag ingredient displays to the client. Tag and list ingredients
        // become a Composite and are silently dropped. The server must drop the same recipes so the
        // client button index keeps pointing at the same recipe.
        val tagRecipe =
            StonecutterRecipe(
                IngredientOrIngredients.Ingredient(
                    RecipeIngredient.Tag(Key.key("minecraft:logs"), RecipeFactory.itemController),
                ),
                ItemStack.of(Material.OAK_PLANKS),
                null,
            )
        val listRecipe =
            StonecutterRecipe(
                IngredientOrIngredients.Ingredients(
                    listOf(
                        RecipeIngredient.MaterialItem(Key.key("minecraft:oak_log"), RecipeFactory.itemController),
                        RecipeIngredient.MaterialItem(Key.key("minecraft:birch_log"), RecipeFactory.itemController),
                    ),
                ),
                ItemStack.of(Material.STICK),
                null,
            )
        val materialRecipe =
            StonecutterRecipe(ingredient(Material.OAK_LOG), ItemStack.of(Material.OAK_STAIRS), null)

        assertTrue(tagRecipe.ingredientDisplay() is SlotDisplay.Composite)
        assertTrue(listRecipe.ingredientDisplay() is SlotDisplay.Composite)
        assertTrue(materialRecipe.ingredientDisplay() is SlotDisplay.Item)

        MinecraftServer.getRecipeManager().addRecipe(tagRecipe)
        MinecraftServer.getRecipeManager().addRecipe(listRecipe)
        MinecraftServer.getRecipeManager().addRecipe(materialRecipe)

        val entries = stonecutterEntries()
        assertEquals(1, entries.size, "only the material recipe must reach the client")
        assertFalse(entries.any { it.optionDisplay().toString().contains("stick") })

        val inventory = StonecutterInventory()
        inventory.setItemStack(inventory.inputSlot, ItemStack.of(Material.OAK_LOG))
        EventDispatcher.call(InventoryButtonClickEvent(mock<Player>(), inventory, 0))
        assertEquals(Material.OAK_STAIRS, inventory.getItemStack(inventory.outputSlot).material())
    }
}
