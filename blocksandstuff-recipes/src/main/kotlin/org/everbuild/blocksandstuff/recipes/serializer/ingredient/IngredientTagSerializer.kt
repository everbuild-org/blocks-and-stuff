package org.everbuild.blocksandstuff.recipes.serializer.ingredient

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key
import org.everbuild.blocksandstuff.recipes.api.ItemController

class IngredientTagSerializer(val itemController: ItemController) : KSerializer<RecipeIngredient.Tag> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IngredientTag", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RecipeIngredient.Tag =
        RecipeIngredient.Tag(Key.key(decoder.decodeString().removePrefix("#")), itemController)

    override fun serialize(encoder: Encoder, value: RecipeIngredient.Tag) {
        encoder.encodeString("#${value.value.asString()}")
    }
}