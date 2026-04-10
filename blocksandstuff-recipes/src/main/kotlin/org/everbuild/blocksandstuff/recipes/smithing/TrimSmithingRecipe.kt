package org.everbuild.blocksandstuff.recipes.smithing

import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.armor.TrimMaterial
import net.minestom.server.item.armor.TrimPattern
import net.minestom.server.item.component.ArmorTrim
import org.everbuild.blocksandstuff.recipes.loader.RecipeModel
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients

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
        addition: ItemStack?
    ): ItemStack {
        if (addition == null) return base
        if (template == null) return base
        val trimPattern = when(template.material()) {
            Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.BOLT
            Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SENTRY
            Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.DUNE
            Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.COAST
            Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.WILD
            Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.WARD
            Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.EYE
            Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.VEX
            Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.TIDE
            Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SNOUT
            Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.RIB
            Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SPIRE
            Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.WAYFINDER
            Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SHAPER
            Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.SILENCE
            Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.RAISER
            Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.HOST
            Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE -> TrimPattern.FLOW
            else -> return base
        }

        val trimMaterial = when(addition.material()) {
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