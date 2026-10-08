package org.everbuild.blocksandstuff.recipes

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.everbuild.blocksandstuff.recipes.api.ItemController
import org.everbuild.blocksandstuff.recipes.impl.ItemControllerImpl
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.IngredientItemStackSerializer
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.IngredientMaterialSerializer
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.IngredientTagSerializer
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredient
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredientSerializer
import org.everbuild.blocksandstuff.recipes.serializer.stack.ItemStackRecipeResult
import org.everbuild.blocksandstuff.recipes.serializer.stack.ItemStackResultSerializer

object SerializationFactory {

    private var controller: ItemController = ItemControllerImpl

    fun json(itemController: ItemController = ItemControllerImpl): Json {
        this.controller = itemController

        return Json {
            serializersModule = SerializersModule {
                contextual(
                    ItemStackRecipeResult::class,
                    ItemStackResultSerializer(controller)
                )
                contextual(
                    RecipeIngredient.MaterialItem::class,
                    IngredientMaterialSerializer(controller)
                )
                contextual(
                    RecipeIngredient.Tag::class,
                    IngredientTagSerializer(controller)
                )
                contextual(
                    RecipeIngredient.Stack::class,
                    IngredientItemStackSerializer(controller)
                )
                contextual(
                    RecipeIngredient::class,
                    RecipeIngredientSerializer(controller)
                )
            }
        }
    }
}
