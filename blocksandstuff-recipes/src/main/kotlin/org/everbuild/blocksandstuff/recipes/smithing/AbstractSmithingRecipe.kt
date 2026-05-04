package org.everbuild.blocksandstuff.recipes.smithing

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.Ingredient
import net.minestom.server.recipe.Recipe
import net.minestom.server.recipe.RecipeBookCategory
import net.minestom.server.recipe.RecipeProperty
import net.minestom.server.recipe.display.RecipeDisplay
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients

abstract class AbstractSmithingRecipe(
    val template: IngredientOrIngredients? = null,
    val base: IngredientOrIngredients,
    val addition: IngredientOrIngredients? = null,
) : Recipe {
    override fun craftingRequirements(): List<Ingredient> = emptyList()

    override fun createRecipeDisplays(): List<RecipeDisplay> = listOf()

    override fun itemProperties(): Map<RecipeProperty, List<Material>> {
        val result = mutableMapOf<RecipeProperty, List<Material>>()
        if (template != null) result[RecipeProperty.SMITHING_TEMPLATE] = template.asMaterials()
        result[RecipeProperty.SMITHING_BASE] = base.asMaterials()
        if (addition != null) result[RecipeProperty.SMITHING_ADDITION] = addition.asMaterials()
        return result
    }

    override fun recipeBookCategory(): RecipeBookCategory? = RecipeBookCategory.SMITHING
    override fun recipeBookGroup(): String? = null

    open fun matches(template: ItemStack?, base: ItemStack, addition: ItemStack?): Boolean {
        val templateMatches = this.template?.matches(template ?: ItemStack.AIR) ?: true
        val baseMatches = this.base.matches(base)
        val additionMatches = this.addition?.matches(addition ?: ItemStack.AIR) ?: true

        return templateMatches && baseMatches && additionMatches
    }

    abstract fun getResult(template: ItemStack?, base: ItemStack, addition: ItemStack?): ItemStack
}