package org.everbuild.blocksandstuff.recipes.smelting

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.RecipeBookCategory
import org.everbuild.blocksandstuff.recipes.ingredient
import org.everbuild.blocksandstuff.recipes.initRecipeServer
import org.everbuild.blocksandstuff.recipes.smelting.blast_furnace.BlastFurnaceRecipe
import org.everbuild.blocksandstuff.recipes.smelting.furnace.FurnaceRecipe
import org.everbuild.blocksandstuff.recipes.smelting.smoker.SmokerRecipe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SmeltingRecipeTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = initRecipeServer()
    }

    @Test
    fun `furnace recipe matches only its input`() {
        val recipe =
            FurnaceRecipe(
                ingredient(Material.RAW_IRON),
                ItemStack.of(Material.IRON_INGOT),
                0.7f,
                null,
                RecipeBookCategory.FURNACE_MISC,
                200,
            )

        assertTrue(recipe.matches(ItemStack.of(Material.RAW_IRON)))
        assertFalse(recipe.matches(ItemStack.of(Material.RAW_GOLD)))
        assertEquals(Material.IRON_INGOT, recipe.result.material())
        assertEquals(200, recipe.burnTime)
    }

    @Test
    fun `blast furnace and smoker recipes match their inputs`() {
        val blasting =
            BlastFurnaceRecipe(
                ingredient(Material.RAW_IRON),
                ItemStack.of(Material.IRON_INGOT),
                0.7f,
                null,
                RecipeBookCategory.FURNACE_MISC,
                100,
            )
        val smoking =
            SmokerRecipe(
                ingredient(Material.BEEF),
                ItemStack.of(Material.COOKED_BEEF),
                0.35f,
                null,
                RecipeBookCategory.FURNACE_FOOD,
                100,
            )

        assertTrue(blasting.matches(ItemStack.of(Material.RAW_IRON)))
        assertTrue(smoking.matches(ItemStack.of(Material.BEEF)))
        assertFalse(smoking.matches(ItemStack.of(Material.RAW_IRON)))
    }
}
