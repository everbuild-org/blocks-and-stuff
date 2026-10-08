package org.everbuild.blocksandstuff.recipes.grid

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.Ingredient
import net.minestom.server.recipe.Recipe
import net.minestom.server.recipe.RecipeBookCategory
import net.minestom.server.recipe.RecipeProperty
import net.minestom.server.recipe.display.RecipeDisplay
import net.minestom.server.recipe.display.SlotDisplay
import org.everbuild.blocksandstuff.recipes.loader.RecipeModel
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients
import org.everbuild.blocksandstuff.recipes.util.RecipeBookActivity
import org.everbuild.blocksandstuff.recipes.util.findRecipeCategory

data class ShapelessCraftingRecipe(
    val category: RecipeBookCategory,
    val group: String?,
    val ingredients: List<IngredientOrIngredients>,
    val result: ItemStack
) : Recipe, CraftingGridRecipe {
    constructor(recipe: RecipeModel.ShapelessCraftingRecipe) : this(
        findRecipeCategory(RecipeBookActivity.CRAFTING, recipe.category),
        recipe.group,
        recipe.ingredients,
        recipe.result.item,
    )

    override fun createRecipeDisplays(): List<RecipeDisplay> {
        return listOf(
            RecipeDisplay.CraftingShapeless(
                ingredients.map { it.asSlotDisplay() },
                SlotDisplay.ItemStack(result),
                SlotDisplay.Item(Material.CRAFTING_TABLE)
            )
        )
    }

    override fun craftingRequirements(): List<Ingredient> {
        return ingredients.mapNotNull { it.asIngredient() }
    }

    override fun recipeBookCategory(): RecipeBookCategory = category
    override fun recipeBookGroup(): String? = group
    override fun itemProperties(): Map<RecipeProperty, List<Material>> {
        return mapOf()
    }

    override fun getResult(source: GridPattern<ItemStack>): ItemStack = result
    override fun matches(pattern: GridPattern<ItemStack>): Boolean {
        val items = pattern
            .filterInvalid()
            .grid
            .flatten()
            .mapNotNull { if (it == ItemStack.AIR) null else it }
            .toMutableList()

        val ingredients = ingredients.toMutableList()

        for (item in items.toList()) {
            val ingredient = ingredients.find { it.matches(item) } ?: return false
            ingredients.remove(ingredient)
            items.remove(item)
        }

        return items.isEmpty() && ingredients.isEmpty()
    }

    override fun takeOne(pattern: GridPattern<ItemStack>): GridPattern<ItemStack> {
        val newGrid = pattern.grid.map { row ->
            row.map { item ->
                if (item == ItemStack.AIR || item == null) return@map ItemStack.AIR
                val ingredient = ingredients.find { it.matches(item) }
                    ?: return@map item
                item.withAmount(item.amount() - ingredient.amount(item))
            }.toMutableList()
        }.toMutableList()

        return GridPattern(newGrid, pattern.minRow, pattern.minCol, ItemStack.AIR)
    }
}