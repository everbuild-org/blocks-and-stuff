package org.everbuild.blocksandstuff.recipes.serializer.ingredient

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key
import org.everbuild.blocksandstuff.recipes.api.ItemController

class IngredientMaterialSerializer(val itemController: ItemController) : KSerializer<RecipeIngredient.MaterialItem> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IngredientItem", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RecipeIngredient.MaterialItem =
        RecipeIngredient.MaterialItem(Key.key(decoder.decodeString()), itemController)

    override fun serialize(encoder: Encoder, value: RecipeIngredient.MaterialItem) {
        encoder.encodeString(value.value.asString())
    }
}