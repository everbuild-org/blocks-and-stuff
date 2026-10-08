package org.everbuild.blocksandstuff.recipes.serializer.ingredient

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.kyori.adventure.key.Key
import org.everbuild.blocksandstuff.recipes.api.ItemController

class RecipeIngredientSerializer(val itemController: ItemController) : JsonContentPolymorphicSerializer<RecipeIngredient>(
    RecipeIngredient::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<RecipeIngredient> {
        try {
            if (element is JsonObject) return IngredientItemStackSerializer(itemController)
            if (element.jsonPrimitive.content.startsWith("#")) return IngredientTagSerializer(itemController)
            if (Key.key(element.jsonPrimitive.content).namespace() != "minecraft") return IngredientCustomItemSerializer(
                itemController
            )
            return IngredientMaterialSerializer(itemController)
        } catch (e: NullPointerException) {
            throw RuntimeException("Can not parse ingredient: ${element.jsonPrimitive.content}", e)
        }
    }
}