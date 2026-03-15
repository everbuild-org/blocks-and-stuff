package org.everbuild.blocksandstuff.recipes.smithing

import net.kyori.adventure.key.Key
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.armor.TrimMaterial
import net.minestom.server.item.armor.TrimPattern
import net.minestom.server.item.component.ArmorTrim
import net.minestom.server.registry.RegistryKey
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
            Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:bolt"))
            Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:sentry"))
            Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:dune"))
            Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:coast"))
            Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:wild"))
            Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:ward"))
            Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:eye"))
            Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:vex"))
            Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:tide"))
            Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:snout"))
            Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:rib"))
            Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:spire"))
            Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:wayfinder"))
            Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:shaper"))
            Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:silence"))
            Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:raiser"))
            Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:host"))
            Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE -> RegistryKey.unsafeOf<TrimPattern>(Key.key("minecraft:flow"))
            else -> return base
        }

        val trimMaterial = when(addition.material()) {
            Material.AMETHYST_SHARD -> RegistryKey.unsafeOf(Key.key("minecraft:amethyst"))
            Material.COPPER_INGOT -> RegistryKey.unsafeOf(Key.key("minecraft:copper"))
            Material.GOLD_INGOT -> RegistryKey.unsafeOf(Key.key("minecraft:gold"))
            Material.EMERALD -> RegistryKey.unsafeOf(Key.key("minecraft:emerald"))
            Material.DIAMOND -> RegistryKey.unsafeOf(Key.key("minecraft:diamond"))
            Material.IRON_INGOT -> RegistryKey.unsafeOf(Key.key("minecraft:iron"))
            Material.LAPIS_LAZULI -> RegistryKey.unsafeOf(Key.key("minecraft:lapis"))
            Material.NETHERITE_INGOT -> RegistryKey.unsafeOf(Key.key("minecraft:netherite"))
            Material.QUARTZ -> RegistryKey.unsafeOf(Key.key("minecraft:quartz"))
            Material.REDSTONE -> RegistryKey.unsafeOf<TrimMaterial>(Key.key("minecraft:redstone"))
            else -> return base
        }

        return base.with(DataComponents.TRIM, ArmorTrim(trimMaterial, trimPattern))
    }
}