package org.everbuild.blocksandstuff.recipes.serializer.stack

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import net.kyori.adventure.key.Key
import net.minestom.server.component.DataComponent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.NetworkBuffer
import org.everbuild.blocksandstuff.recipes.SerializationFactory
import org.everbuild.blocksandstuff.recipes.api.ItemController
import org.everbuild.blocksandstuff.recipes.impl.ItemControllerImpl.withAnonymous

class ItemStackResultSerializer(val itemController: ItemController) : KSerializer<ItemStackRecipeResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CraftingRecipeResult") {
        element<String>("id")
        element<Int>("count", isOptional = true)
        element<JsonObject>("components", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: ItemStackRecipeResult) {
        require(encoder is JsonEncoder)
        val jsonObject = buildJsonObject {
            put("id", JsonPrimitive(value.item.material().name()))
            put("count", JsonPrimitive(value.item.amount()))
        }
        encoder.encodeJsonElement(jsonObject)
    }

    @OptIn(InternalSerializationApi::class)
    override fun deserialize(decoder: Decoder): ItemStackRecipeResult {
        require(decoder is JsonDecoder)
        val jsonObject = decoder.decodeJsonElement().jsonObject

        val id = jsonObject["id"]?.jsonPrimitive?.content ?: throw SerializationException("Missing 'id'")

        val count = jsonObject["count"]?.jsonPrimitive?.intOrNull
        val data = mutableMapOf<DataComponent<*>, Any>()
        val customData = mutableListOf<Any>()

        jsonObject["components"]?.jsonObject?.let { components ->
            for ((key, value) in components) {
                val itemComponent = DataComponent.values().find { it.name() == key }
                if (itemComponent != null) {
                    val nbt = value.asNbt()
                    val buf = NetworkBuffer.resizableBuffer()
                    buf.write(NetworkBuffer.NBT, nbt)
                    data[itemComponent] = itemComponent.read(buf)
                }

                val customComponent = itemController.getComponentClassByKey(Key.key(key))
                if (customComponent != null) {
                    customData.add(
                        SerializationFactory.json()
                            .decodeFromJsonElement(customComponent.serializer(), value)
                    )
                }
            }
        }

        val material = Material.fromKey(id)
        var item = if (material != null) {
            ItemStack.of(material, count ?: 1)
        } else {
            (itemController.byKey(Key.key(id)) ?: throw SerializationException("Unknown item '$id'"))
        }

        for ((component, value) in data) {
            @Suppress("UNCHECKED_CAST") // always succeeds
            item = item.with(component as DataComponent<Any>, value)
        }

        for (component in customData) {
            item = item.withAnonymous(component)
        }

        return ItemStackRecipeResult(item)
    }
}