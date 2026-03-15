package org.everbuild.blocksandstuff.recipes.smelting.blast_furnace

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.recipes.smelting.AbstractSmeltingHandler

class BlastFurnaceHandler : AbstractSmeltingHandler(BlastFurnaceRecipe::class.java) {
    override fun getKey(): Key = Key.key("minecraft:blast_furnace")
    override val inventoryTitle = Component.translatable("container.${key.value()}")
    override val inventoryType: InventoryType = InventoryType.BLAST_FURNACE

    override fun takeFuel(instance: Instance, blockPosition: Point): Int {
        return super.takeFuel(instance, blockPosition) / 2
    }
}