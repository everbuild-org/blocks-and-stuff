package org.everbuild.blocksandstuff.recipes.smelting.smoker

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.recipes.smelting.AbstractSmeltingHandler

class SmokerHandler : AbstractSmeltingHandler(SmokerRecipe::class.java) {
    override fun getKey(): Key = Key.key("minecraft:smoker")
    override val inventoryType: InventoryType = InventoryType.SMOKER
    override val inventoryTitle = Component.translatable("container.${key.value()}")

    override fun takeFuel(instance: Instance, blockPosition: Point): Int {
        return super.takeFuel(instance, blockPosition) / 2
    }
}