package org.everbuild.blocksandstuff.recipes.smelting.smoker

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.recipes.smelting.AbstractSmeltingHandler
import org.everbuild.blocksandstuff.recipes.smelting.FurnaceArchetype

class SmokerHandler : AbstractSmeltingHandler(
    SmokerRecipe::class.java,
    Archetype
) {
    override fun getKey(): Key = Key.key("minecraft:smoker")

    override fun takeFuel(instance: Instance, blockPosition: Point): Int {
        return super.takeFuel(instance, blockPosition) / 2
    }

    private object Archetype : FurnaceArchetype(
        title = Component.translatable("container.smoker"),
        inventoryType = InventoryType.SMOKER
    )
}