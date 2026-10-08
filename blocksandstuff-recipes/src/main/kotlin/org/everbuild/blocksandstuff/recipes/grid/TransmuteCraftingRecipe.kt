package org.everbuild.blocksandstuff.recipes.grid

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.Ingredient
import net.minestom.server.recipe.Recipe
import net.minestom.server.recipe.RecipeBookCategory
import net.minestom.server.recipe.RecipeProperty
import net.minestom.server.recipe.display.RecipeDisplay
import org.everbuild.blocksandstuff.recipes.loader.RecipeModel
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredient
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients
import org.everbuild.blocksandstuff.recipes.util.RecipeBookActivity
import org.everbuild.blocksandstuff.recipes.util.findRecipeCategory

data class TransmuteCraftingRecipe(
    val category: RecipeBookCategory,
    val group: String?,
    val ingredient: IngredientOrIngredients,
    val extra: IngredientOrIngredients,
    val resultIngredient: RecipeIngredient
) : Recipe, CraftingGridRecipe {
    constructor(recipe: RecipeModel.TransmuteCraftingRecipe) : this(
        findRecipeCategory(RecipeBookActivity.CRAFTING, recipe.category),
        recipe.group,
        recipe.input,
        recipe.material,
        recipe.result,
    )

    override fun createRecipeDisplays(): List<RecipeDisplay> = emptyList()
    override fun craftingRequirements(): List<Ingredient> = emptyList()
    override fun recipeBookCategory(): RecipeBookCategory = category
    override fun recipeBookGroup(): String? = group
    override fun itemProperties(): Map<RecipeProperty, List<Material>> = emptyMap()

    private fun getResultOrNull(source: GridPattern<ItemStack>): ItemStack? {
        val items = source
            .filterInvalid()
            .grid
            .flatten()
            .mapNotNull { if (it == ItemStack.AIR) null else it }
            .toMutableList()

        if (items.size != 2) return null

        val sourceItem = items.find { ingredient.matches(it) } ?: return null
        items.find { extra.matches(it) } ?: return null

        return resultIngredient.transmute(sourceItem)
    }

    override fun getResult(source: GridPattern<ItemStack>): ItemStack {
        return getResultOrNull(source) ?: ItemStack.AIR
    }

    override fun matches(pattern: GridPattern<ItemStack>): Boolean {
        return getResultOrNull(pattern) != null
    }

    override fun takeOne(pattern: GridPattern<ItemStack>): GridPattern<ItemStack> {
        val newGrid = pattern.grid.map { row ->
            row.map { item ->
                if (item == ItemStack.AIR || item == null) return@map ItemStack.AIR
                val isIngredient = ingredient.matches(item)
                val isExtra = extra.matches(item)

                if (isIngredient) {
                    return@map item.withAmount { item.amount() - ingredient.amount(item) }
                } else if (isExtra) {
                    return@map item.withAmount { item.amount() - extra.amount(item) }
                } else {
                    return@map item
                }
            }.toMutableList()
        }.toMutableList()

        return GridPattern(newGrid, pattern.minRow, pattern.minCol, ItemStack.AIR)
    }
}