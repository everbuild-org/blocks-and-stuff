package org.everbuild.blocksandstuff.recipes.stonecutting

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.Recipe
import net.minestom.server.recipe.RecipeBookCategory
import net.minestom.server.recipe.RecipeProperty
import net.minestom.server.recipe.display.RecipeDisplay
import net.minestom.server.recipe.display.SlotDisplay
import org.everbuild.blocksandstuff.recipes.loader.RecipeModel
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients

class StonecutterRecipe(
    private val input: IngredientOrIngredients,
    override val result: ItemStack,
    private val group: String?,
) : Recipe, StonecuttingRecipe {

    constructor(recipe: RecipeModel.StonecuttingRecipe) : this(
        recipe.ingredient,
        recipe.result.item,
        recipe.group
    )

    override fun matches(itemStack: ItemStack): Boolean = input.matches(itemStack)

    override fun itemProperties(): MutableMap<RecipeProperty, MutableList<Material>> {
        return mutableMapOf()
    }

    override fun recipeBookGroup(): String? {
        return group
    }

    override fun recipeBookCategory(): RecipeBookCategory {
        return RecipeBookCategory.STONECUTTER
    }

    override fun createRecipeDisplays(): List<RecipeDisplay?> {
        return listOf(
            RecipeDisplay.Stonecutter(
                input.asSlotDisplay(), SlotDisplay.ItemStack(result), SlotDisplay.ItemStack(
                    ItemStack.of(Material.STONECUTTER)
                )
            )
        )
    }
}