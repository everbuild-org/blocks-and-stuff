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

class ShapedCraftingRecipe(
    grid: GridPattern<IngredientOrIngredients>,
    val result: ItemStack,
    private val group: String?,
    private val category: RecipeBookCategory,
) : Recipe, CraftingGridRecipe {
    constructor(recipe: RecipeModel.ShapedCraftingRecipe) : this(
        GridPattern.fromRecipeDefinition(recipe.pattern, recipe.key),
        recipe.result.item,
        recipe.group,
        findRecipeCategory(RecipeBookActivity.CRAFTING, recipe.category)
    )

    private val grid: GridPattern<IngredientOrIngredients> = grid.minimizePattern()
    private val width: Int get() = this.grid.width
    private val height: Int get() = this.grid.height

    override fun getResult(source: GridPattern<ItemStack>): ItemStack = result
    override fun matches(pattern: GridPattern<ItemStack>): Boolean = grid.matches(pattern)
    override fun takeOne(pattern: GridPattern<ItemStack>): GridPattern<ItemStack>? {
        assert(pattern.width == width && pattern.height == height)

        val recipeGrid = grid.grid.flatten()
        val newItems = pattern.grid.flatten().mapIndexed { index, itemStack ->
            val ingredient = recipeGrid[index] ?: return@mapIndexed itemStack
            itemStack ?: return@mapIndexed null
            val neededAmount = ingredient.amount(itemStack)
            if (itemStack.amount() >= neededAmount) {
                itemStack.withAmount(itemStack.amount() - neededAmount)
            } else {
                return null
            }
        }

        return GridPattern(
            newItems
                .windowed(width, width),
            pattern.minRow, pattern.minCol,
            ItemStack.AIR
        )
    }

    override fun createRecipeDisplays(): List<RecipeDisplay> {
        return listOf<RecipeDisplay>(
            RecipeDisplay.CraftingShaped(
                grid.width,
                grid.height,
                grid.ingredients.map { it.asSlotDisplay() },
                SlotDisplay.ItemStack(result),
                SlotDisplay.Item(Material.CRAFTING_TABLE),
            )
        )
    }

    override fun itemProperties(): MutableMap<RecipeProperty, MutableList<Material>> {
        return mutableMapOf()
    }

    override fun recipeBookGroup(): String? {
        return group
    }

    override fun recipeBookCategory(): RecipeBookCategory {
        return category
    }

    override fun craftingRequirements(): List<Ingredient> = grid.ingredients
        .filter { it.hasDataAttached() }
        .mapNotNull { it.asIngredient() }
}