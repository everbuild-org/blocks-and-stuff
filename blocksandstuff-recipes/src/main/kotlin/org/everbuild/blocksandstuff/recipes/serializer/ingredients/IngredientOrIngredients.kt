package org.everbuild.blocksandstuff.recipes.serializer.ingredients

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.display.SlotDisplay
import org.everbuild.blocksandstuff.recipes.api.ItemController
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredient
import net.minestom.server.recipe.Ingredient as MinestomIngredient

@Serializable(with = IngredientListSerializer::class)
sealed interface IngredientOrIngredients {
    fun asSlotDisplay(): SlotDisplay
    fun asIngredient(): MinestomIngredient?
    fun hasDataAttached(): Boolean
    fun matches(itemStack: ItemStack): Boolean
    fun amount(itemStack: ItemStack): Int
    fun asMaterials(): List<Material>

    @JvmInline
    @Serializable
    value class Ingredient(@Contextual val ingredient: RecipeIngredient) : IngredientOrIngredients {

        override fun asSlotDisplay(): SlotDisplay = ingredient.asSlotDisplay()

        override fun asIngredient(): MinestomIngredient? {
            val ingredients = ingredient.asIngredients()
                .flatMap { it.items }

            if (ingredients.isEmpty()) return null
            return MinestomIngredient(ingredients)
        }

        override fun hasDataAttached(): Boolean = ingredient.hasDataAttached()
        override fun matches(itemStack: ItemStack): Boolean = ingredient.matches(itemStack)
        override fun amount(itemStack: ItemStack): Int = ingredient.amount(itemStack)
        override fun asMaterials(): List<Material> = ingredient.asMaterials()
    }

    @JvmInline
    @Serializable
    value class Ingredients(val ingredients: List<@Contextual RecipeIngredient>) : IngredientOrIngredients {
        override fun asSlotDisplay(): SlotDisplay = SlotDisplay.Composite(ingredients.map { it.asSlotDisplay() })

        override fun asIngredient(): MinestomIngredient? {
            val ingredients = ingredients
                .flatMap { it.asIngredients() }
                .flatMap { it.items }

            if (ingredients.isEmpty()) return null
            return MinestomIngredient(ingredients)
        }

        override fun hasDataAttached(): Boolean = ingredients.any { it.hasDataAttached() }
        override fun matches(itemStack: ItemStack): Boolean = ingredients.any { it.matches(itemStack) }
        override fun amount(itemStack: ItemStack): Int = ingredients.find { it.matches(itemStack) }?.amount(itemStack) ?: 0
        override fun asMaterials(): List<Material> = ingredients.flatMap { it.asMaterials() }
    }

    companion object {
        fun of(it: ItemStack, itemController: ItemController): IngredientOrIngredients =
            Ingredient(RecipeIngredient.MaterialItem(it.material().key(), itemController))

        fun air(itemController: ItemController) =
            Ingredient(RecipeIngredient.MaterialItem(Material.AIR.key(), itemController))
    }
}