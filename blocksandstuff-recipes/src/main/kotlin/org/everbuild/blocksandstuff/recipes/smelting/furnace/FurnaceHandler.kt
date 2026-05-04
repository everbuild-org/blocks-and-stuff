package org.everbuild.blocksandstuff.recipes.smelting.furnace

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.recipes.smelting.AbstractSmeltingHandler
import org.everbuild.blocksandstuff.recipes.smelting.FurnaceArchetype

class FurnaceHandler : AbstractSmeltingHandler(
    FurnaceRecipe::class.java,
    Archetype
) {
    override fun getKey(): Key = Key.key("minecraft:furnace")

    private object Archetype : FurnaceArchetype(
        title = Component.translatable("container.furnace"),
        inventoryType = InventoryType.FURNACE
    )
}