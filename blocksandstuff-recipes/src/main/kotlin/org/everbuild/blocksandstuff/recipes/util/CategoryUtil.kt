package org.everbuild.blocksandstuff.recipes.util

import net.minestom.server.recipe.RecipeBookCategory

enum class RecipeBookActivity {
    CRAFTING,
    SMELTING
}

fun findRecipeCategory(namespace: RecipeBookActivity, category: String): RecipeBookCategory {
    return when (namespace) {
        RecipeBookActivity.CRAFTING -> when (category) {
            "equipment" -> RecipeBookCategory.CRAFTING_EQUIPMENT
            "building" -> RecipeBookCategory.CRAFTING_BUILDING_BLOCKS
            "misc" -> RecipeBookCategory.CRAFTING_MISC
            else -> RecipeBookCategory.CRAFTING_MISC
        }

        RecipeBookActivity.SMELTING -> when (category) {
            "food" -> RecipeBookCategory.FURNACE_FOOD
            "data/everbuild/blocks" -> RecipeBookCategory.FURNACE_BLOCKS
            else -> RecipeBookCategory.FURNACE_MISC
        }
    }
}