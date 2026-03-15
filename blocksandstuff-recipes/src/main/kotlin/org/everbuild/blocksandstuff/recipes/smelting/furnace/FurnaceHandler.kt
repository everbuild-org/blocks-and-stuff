package org.everbuild.blocksandstuff.recipes.smelting.furnace

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.recipes.smelting.AbstractSmeltingHandler

class FurnaceHandler : AbstractSmeltingHandler(FurnaceRecipe::class.java) {
    override fun getKey(): Key = Key.key("minecraft:furnace")
    override val inventoryType: InventoryType = InventoryType.FURNACE
    override val inventoryTitle: TranslatableComponent = Component.translatable("container.furnace")
}