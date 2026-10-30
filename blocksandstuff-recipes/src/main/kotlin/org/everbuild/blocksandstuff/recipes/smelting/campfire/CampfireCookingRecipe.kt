package org.everbuild.blocksandstuff.recipes.smelting.campfire

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.Recipe
import net.minestom.server.recipe.RecipeBookCategory
import net.minestom.server.recipe.RecipeProperty
import org.everbuild.blocksandstuff.recipes.loader.RecipeModel
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients
import org.everbuild.blocksandstuff.recipes.smelting.SmeltingRecipe
import org.everbuild.blocksandstuff.recipes.util.RecipeBookActivity
import org.everbuild.blocksandstuff.recipes.util.findRecipeCategory

class CampfireCookingRecipe(
    private val input: IngredientOrIngredients,
    override val result: ItemStack,
    override val experience: Float,
    private val group: String?,
    private val category: RecipeBookCategory,
    override val burnTime: Int,
) : Recipe,
    SmeltingRecipe {
    constructor(recipe: RecipeModel.CampfireCookingRecipe) : this(
        recipe.ingredient,
        recipe.result.item,
        recipe.experience.toFloat(),
        recipe.group,
        findRecipeCategory(RecipeBookActivity.SMELTING, recipe.category),
        recipe.cookingTime,
    )

    override fun matches(itemStack: ItemStack): Boolean = input.matches(itemStack)

    override fun itemProperties(): MutableMap<RecipeProperty, MutableList<Material>> = mutableMapOf()

    override fun recipeBookGroup(): String? = group

    override fun recipeBookCategory(): RecipeBookCategory = category
}
