package org.everbuild.blocksandstuff.recipes.smelting.blast_furnace

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.recipes.smelting.AbstractSmeltingHandler
import org.everbuild.blocksandstuff.recipes.smelting.FurnaceArchetype

class BlastFurnaceHandler : AbstractSmeltingHandler(
    BlastFurnaceRecipe::class.java,
    Archetype
) {
    override fun getKey(): Key = Key.key("minecraft:blast_furnace")

    override fun takeFuel(instance: Instance, blockPosition: Point): Int {
        return super.takeFuel(instance, blockPosition) / 2
    }

    private object Archetype : FurnaceArchetype(
        title = Component.translatable("container.blast_furnace"),
        inventoryType = InventoryType.BLAST_FURNACE
    )
}