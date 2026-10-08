@file:OptIn(ExperimentalSerializationApi::class)

package org.everbuild.blocksandstuff.recipes.loader

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredient
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients
import org.everbuild.blocksandstuff.recipes.serializer.stack.ItemStackRecipeResult

@Serializable
@JsonClassDiscriminator("type")
sealed class RecipeModel {
    abstract val type: String

    @Serializable
    @SerialName("minecraft:crafting_shaped")
    data class ShapedCraftingRecipe(
        override val type: String,
        val category: String,
        val group: String? = null,
        @SerialName("show_notification") val showNotification: Boolean = true,
        val pattern: List<String>,
        val key: Map<String, IngredientOrIngredients>,
        @Contextual val result: ItemStackRecipeResult
    ) : RecipeModel()

    @Serializable
    @SerialName("minecraft:crafting_shapeless")
    data class ShapelessCraftingRecipe(
        override val type: String,
        val category: String,
        val group: String? = null,
        val ingredients: List<IngredientOrIngredients>,
        @Contextual val result: ItemStackRecipeResult
    ) : RecipeModel()

    @Serializable
    @SerialName("minecraft:crafting_transmute")
    data class TransmuteCraftingRecipe(
        override val type: String,
        val category: String,
        val group: String? = null,
        val input: IngredientOrIngredients,
        val material: IngredientOrIngredients,
        @Contextual val result: RecipeIngredient
    ) : RecipeModel()

    @Serializable
    @SerialName("minecraft:smithing_transform")
    data class TransformSmithingRecipe(
        override val type: String,
        val template: IngredientOrIngredients? = null,
        val base: IngredientOrIngredients,
        val addition: IngredientOrIngredients? = null,
        @Contextual val result: RecipeIngredient
    ) : RecipeModel()

    @Serializable
    @SerialName("minecraft:smithing_trim")
    data class TrimSmithingRecipe(
        override val type: String,
        val template: IngredientOrIngredients? = null,
        val base: IngredientOrIngredients,
        val addition: IngredientOrIngredients? = null,
    ) : RecipeModel()

    @Serializable
    @SerialName("minecraft:smelting")
    data class SmeltingRecipe(
        override val type: String,
        val category: String,
        val group: String? = null,
        @SerialName("cookingtime") val cookingTime: Int = 200,
        val ingredient: IngredientOrIngredients,
        val experience: Double = 0.0,
        @Contextual val result: ItemStackRecipeResult
    ) : RecipeModel()

    @Serializable
    @SerialName("minecraft:blasting")
    data class BlastingRecipe(
        override val type: String,
        val category: String,
        val group: String? = null,
        @SerialName("cookingtime") val cookingTime: Int = 100,
        val ingredient: IngredientOrIngredients,
        val experience: Double = 0.0,
        @Contextual val result: ItemStackRecipeResult
    ) : RecipeModel()

    @Serializable
    @SerialName("minecraft:smoking")
    data class SmokingRecipe(
        override val type: String,
        val category: String,
        val group: String? = null,
        @SerialName("cookingtime") val cookingTime: Int = 100,
        val ingredient: IngredientOrIngredients,
        val experience: Double = 0.0,
        @Contextual val result: ItemStackRecipeResult
    ) : RecipeModel()

    @Serializable
    @SerialName("minecraft:stonecutting")
    data class StonecuttingRecipe(
        override val type: String,
        val group: String? = null,
        val ingredient: IngredientOrIngredients,
        @Contextual val result: ItemStackRecipeResult
    ) : RecipeModel()
}
