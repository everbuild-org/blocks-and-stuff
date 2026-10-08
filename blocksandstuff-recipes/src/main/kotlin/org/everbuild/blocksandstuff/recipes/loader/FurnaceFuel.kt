package org.everbuild.blocksandstuff.recipes.loader

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients

@Serializable
@SerialName("minecraft:furnace_fuel")
data class FurnaceFuel(
    @SerialName("item") val itemStack: IngredientOrIngredients,
    @SerialName("burn_time") val burnTime: Int,
)
