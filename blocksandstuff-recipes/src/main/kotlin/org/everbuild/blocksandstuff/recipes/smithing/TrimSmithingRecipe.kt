package org.everbuild.blocksandstuff.recipes.smithing

import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.armor.TrimMaterial
import net.minestom.server.item.component.ArmorTrim
import org.everbuild.blocksandstuff.recipes.loader.RecipeModel
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients
import org.everbuild.blocksandstuff.recipes.util.trimPatterns

class TrimSmithingRecipe(
    template: IngredientOrIngredients? = null,
    base: IngredientOrIngredients,
    addition: IngredientOrIngredients? = null,
) : AbstractSmithingRecipe(template, base, addition) {
    constructor(recipe: RecipeModel.TrimSmithingRecipe) : this(
        recipe.template,
        recipe.base,
        recipe.addition,
    )

    override fun getResult(
        template: ItemStack?,
        base: ItemStack,
        addition: ItemStack?,
    ): ItemStack {
        if (addition == null) return base
        if (template == null) return base
        val trimPattern = trimPatterns[template.material()] ?: return base

        val trimMaterial =
            when (addition.material()) {
                Material.AMETHYST_SHARD -> TrimMaterial.AMETHYST
                Material.COPPER_INGOT -> TrimMaterial.COPPER
                Material.GOLD_INGOT -> TrimMaterial.GOLD
                Material.EMERALD -> TrimMaterial.EMERALD
                Material.DIAMOND -> TrimMaterial.DIAMOND
                Material.IRON_INGOT -> TrimMaterial.IRON
                Material.LAPIS_LAZULI -> TrimMaterial.LAPIS
                Material.NETHERITE_INGOT -> TrimMaterial.NETHERITE
                Material.QUARTZ -> TrimMaterial.QUARTZ
                Material.REDSTONE -> TrimMaterial.REDSTONE
                else -> return base
            }

        return base.with(DataComponents.TRIM, ArmorTrim(trimMaterial, trimPattern))
    }
}
