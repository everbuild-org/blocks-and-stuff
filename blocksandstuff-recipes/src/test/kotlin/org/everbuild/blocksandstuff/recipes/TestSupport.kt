package org.everbuild.blocksandstuff.recipes

import net.minestom.server.MinecraftServer
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.recipes.grid.GridPattern
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients

/**
 * Initializes a fresh Minestom process (registries + recipe manager) for a test class.
 * Calling this replaces the current process, so each test class starts from a clean state.
 */
fun initRecipeServer() {
    MinecraftServer.init()
}

fun ingredient(material: Material, amount: Int = 1): IngredientOrIngredients =
    IngredientOrIngredients.of(ItemStack.of(material, amount), RecipeFactory.itemController)

fun itemGrid(vararg rows: List<ItemStack>): GridPattern<ItemStack> =
    GridPattern(rows.toList(), nullValue = ItemStack.AIR)

fun ingredientGrid(vararg rows: List<IngredientOrIngredients>): GridPattern<IngredientOrIngredients> =
    GridPattern(rows.toList(), nullValue = IngredientOrIngredients.air(RecipeFactory.itemController))
