package org.everbuild.blocksandstuff.recipes.serializer.ingredient

import kotlinx.serialization.Transient
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.component.DataComponent
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.CustomData
import net.minestom.server.recipe.Ingredient
import net.minestom.server.recipe.display.SlotDisplay
import net.minestom.server.registry.TagKey
import org.everbuild.blocksandstuff.recipes.api.ItemController

sealed interface RecipeIngredient {
    fun asSlotDisplay(): SlotDisplay
    fun asIngredients(): List<Ingredient>
    fun hasDataAttached(): Boolean
    fun matches(itemStack: ItemStack): Boolean
    fun amount(itemStack: ItemStack): Int
    fun transmute(source: ItemStack): ItemStack
    fun asMaterials(): List<Material>

    data class MaterialItem(val value: Key, private val itemController: ItemController) : RecipeIngredient {
        @Transient
        val material = Material.fromKey(value)!!
        override fun asSlotDisplay(): SlotDisplay = SlotDisplay.Item(Material.fromKey(value)!!)
        override fun asIngredients(): List<Ingredient> {
            if (value == Key.key("minecraft:air")) return listOf()
            return listOf(Ingredient(Material.fromKey(value)!!))
        }

        override fun hasDataAttached(): Boolean = false
        override fun matches(itemStack: ItemStack): Boolean =
            (!itemController.isCustomItem(itemStack)) && itemStack.material().key() == value

        override fun amount(itemStack: ItemStack): Int = 1
        override fun transmute(source: ItemStack): ItemStack = if (itemController.isCustomItem(source)) {
            source.withTag(itemController.typeTag, null).withMaterial(material)
        } else {
            source.withMaterial(material)
        }

        override fun asMaterials(): List<Material> = listOf(material)
    }

    data class Stack(val value: ItemStack, private val itemController: ItemController) : RecipeIngredient {
        override fun asSlotDisplay(): SlotDisplay = SlotDisplay.ItemStack(value)
        override fun asIngredients(): List<Ingredient> {
            if (value.material() == Material.AIR) return listOf()
            return listOf(Ingredient(value.material()))
        }

        override fun hasDataAttached(): Boolean = value.isSimilar(ItemStack.of(value.material()))
        override fun matches(itemStack: ItemStack): Boolean =
            value.isSimilar(itemStack) && value.amount() <= itemStack.amount()

        override fun amount(itemStack: ItemStack): Int = value.amount()
        override fun transmute(source: ItemStack): ItemStack {
            return if (itemController.isCustomItem(value)) {
                source
                    .with { mergeData(it, value) }
                    .withAmount(
                        (source.amount() * value.amount()).coerceAtMost(value.maxStackSize())
                    )
            } else {
                source
                    .withMaterial(value.material())
                    .with { mergeData(it, value) }
                    .withTag(itemController.typeTag, null)
                    .withAmount(
                        (source.amount() * value.amount()).coerceAtMost(value.maxStackSize())
                    )
            }
        }

        override fun asMaterials(): List<Material> = listOf(value.material())

        private fun mergeData(builder: ItemStack.Builder, source: ItemStack) {
            for (value in source.componentPatch().entrySet()) {
                @Suppress("UNCHECKED_CAST") // can't fail, generic cast
                builder.set(value.component as DataComponent<Any?>, value.value)
            }
            val sourceCustomData = source.get(DataComponents.CUSTOM_DATA)
            val targetCustomData = value.get(DataComponents.CUSTOM_DATA)
            if (sourceCustomData == null || targetCustomData == null) return
            builder.set(
                DataComponents.CUSTOM_DATA, CustomData(
                    CompoundBinaryTag.builder()
                        .put(sourceCustomData.nbt)
                        .put(targetCustomData.nbt)
                        .build()
                )
            )
        }
    }

    data class Tag(val value: Key, private val itemController: ItemController) : RecipeIngredient {
        @Transient
        val tag = try {
            Material.staticRegistry().getTag(value)?.map { it.key() }?.toList() ?: emptyList()
        } catch (e: NullPointerException) {
            throw RuntimeException("Can not parse tag: ${value.asString()}", e)
        }

        override fun asSlotDisplay(): SlotDisplay = SlotDisplay.Tag(TagKey.ofHash("#" + value.asString()))
        override fun asIngredients(): List<Ingredient> {
            val air = Key.key("minecraft:air")
            return tag
                .filterNot { it == air }
                .map { Ingredient(Material.fromKey(it)!!) }
        }

        override fun hasDataAttached(): Boolean = false
        override fun matches(itemStack: ItemStack): Boolean {
            val material = itemStack.material()
            if (itemController.isCustomItem(itemStack)) return false
            if (material == Material.AIR) return false
            return tag.any { it == material.key() }
        }

        override fun amount(itemStack: ItemStack): Int = 1
        override fun transmute(source: ItemStack): ItemStack {
            throw UnsupportedOperationException("transmute not implemented for IngredientTag")
        }

        override fun asMaterials(): List<Material> = tag.map { Material.fromKey(it)!! }
    }
}
