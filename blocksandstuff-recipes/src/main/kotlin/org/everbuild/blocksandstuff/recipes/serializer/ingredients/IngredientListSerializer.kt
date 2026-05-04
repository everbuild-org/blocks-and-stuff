package org.everbuild.blocksandstuff.recipes.serializer.ingredients

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

class IngredientListSerializer :
    JsonContentPolymorphicSerializer<IngredientOrIngredients>(IngredientOrIngredients::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<IngredientOrIngredients> {
        return if (element is JsonArray) IngredientOrIngredients.Ingredients.serializer()
        else IngredientOrIngredients.Ingredient.serializer()
    }
}