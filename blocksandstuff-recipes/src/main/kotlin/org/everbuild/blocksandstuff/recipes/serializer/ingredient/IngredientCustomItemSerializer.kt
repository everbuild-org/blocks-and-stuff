package org.everbuild.blocksandstuff.recipes.serializer.ingredient

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.key.Key
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.recipes.api.ItemController

class IngredientCustomItemSerializer(val itemController: ItemController) : KSerializer<RecipeIngredient.Stack> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IngredientCustomItemStack", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RecipeIngredient.Stack {
        val key = Key.key(decoder.decodeString())
        return RecipeIngredient.Stack(itemController.createCustomItem(key) ?: ItemStack.AIR, itemController)
    }

    override fun serialize(encoder: Encoder, value: RecipeIngredient.Stack) {
        throw UnsupportedOperationException("serialize not implemented")
    }
}