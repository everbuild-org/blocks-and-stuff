package org.everbuild.averium.org.everbuild.blocksandstuff.recipes.smithing

import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.recipes.loader.RecipeModel
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredient
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients
import org.everbuild.blocksandstuff.recipes.smithing.AbstractSmithingRecipe

class TransformSmithingRecipe(
    template: IngredientOrIngredients? = null,
    base: IngredientOrIngredients,
    addition: IngredientOrIngredients? = null,
    private val result: RecipeIngredient
) : AbstractSmithingRecipe(template, base, addition) {
    constructor(recipe: RecipeModel.TransformSmithingRecipe) : this(
        recipe.template,
        recipe.base,
        recipe.addition,
        recipe.result,
    )

    override fun getResult(
        template: ItemStack?,
        base: ItemStack,
        addition: ItemStack?
    ): ItemStack = result.transmute(base)
}