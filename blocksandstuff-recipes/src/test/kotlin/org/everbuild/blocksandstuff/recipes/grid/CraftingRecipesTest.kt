package org.everbuild.blocksandstuff.recipes.grid

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.RecipeBookCategory
import org.everbuild.blocksandstuff.recipes.RecipeFactory
import org.everbuild.blocksandstuff.recipes.ingredient
import org.everbuild.blocksandstuff.recipes.ingredientGrid
import org.everbuild.blocksandstuff.recipes.initRecipeServer
import org.everbuild.blocksandstuff.recipes.itemGrid
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class CraftingRecipesTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = initRecipeServer()
    }

    @Test
    fun `shaped recipe matches result and consumes one per slot`() {
        val planks = ingredient(Material.OAK_PLANKS)
        val recipe =
            ShapedCraftingRecipe(
                ingredientGrid(listOf(planks, planks), listOf(planks, planks)),
                ItemStack.of(Material.CRAFTING_TABLE),
                null,
                RecipeBookCategory.CRAFTING_MISC,
            )

        val stack = ItemStack.of(Material.OAK_PLANKS, 3)
        val grid = itemGrid(listOf(stack, stack), listOf(stack, stack))

        assertTrue(recipe.matches(grid))
        assertEquals(Material.CRAFTING_TABLE, recipe.getResult(grid).material())

        val taken = recipe.takeOne(grid)!!
        assertEquals(2, taken.grid[0][0]!!.amount())
        assertEquals(2, taken.grid[1][1]!!.amount())

        val partial = itemGrid(listOf(ItemStack.of(Material.OAK_PLANKS), ItemStack.AIR), listOf(ItemStack.AIR, ItemStack.AIR))
        assertFalse(recipe.matches(partial), "incomplete shaped pattern must not match")
    }

    @Test
    fun `shapeless recipe matches regardless of order`() {
        val recipe =
            ShapelessCraftingRecipe(
                RecipeBookCategory.CRAFTING_MISC,
                null,
                listOf(ingredient(Material.OAK_LOG)),
                ItemStack.of(Material.OAK_PLANKS, 4),
            )

        val grid = itemGrid(listOf(ItemStack.AIR, ItemStack.of(Material.OAK_LOG)), listOf(ItemStack.AIR, ItemStack.AIR))

        assertTrue(recipe.matches(grid))
        assertEquals(4, recipe.getResult(grid).amount())

        val taken = recipe.takeOne(grid)
        assertTrue(taken.grid[0][1]!!.isAir, "log should be consumed")
        assertFalse(recipe.matches(taken), "empty grid must not match")
    }

    @Test
    fun `transmute recipe combines input and material`() {
        val recipe =
            TransmuteCraftingRecipe(
                RecipeBookCategory.CRAFTING_MISC,
                null,
                ingredient(Material.STICK),
                ingredient(Material.COAL),
                RecipeIngredient.MaterialItem(Material.TORCH.key(), RecipeFactory.itemController),
            )

        val grid = itemGrid(listOf(ItemStack.of(Material.STICK), ItemStack.of(Material.COAL)))

        assertTrue(recipe.matches(grid))
        assertEquals(Material.TORCH, recipe.getResult(grid).material())
        assertFalse(recipe.matches(itemGrid(listOf(ItemStack.of(Material.STICK)))))
    }
}
