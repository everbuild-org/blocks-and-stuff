package org.everbuild.blocksandstuff.recipes.serializer.ingredient

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.everbuild.blocksandstuff.recipes.api.ItemController
import org.everbuild.blocksandstuff.recipes.serializer.stack.ItemStackRecipeResult
import org.everbuild.blocksandstuff.recipes.serializer.stack.ItemStackResultSerializer

class IngredientItemStackSerializer(private val itemController: ItemController) : KSerializer<RecipeIngredient.Stack> {
    private val parent = ItemStackResultSerializer(itemController)

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor("IngredientItemStack", parent.descriptor)

    override fun deserialize(decoder: Decoder): RecipeIngredient.Stack =
        RecipeIngredient.Stack(decoder.decodeSerializableValue(parent).item, itemController)

    override fun serialize(encoder: Encoder, value: RecipeIngredient.Stack) {
        encoder.encodeSerializableValue(parent, ItemStackRecipeResult(value.value))
    }
}