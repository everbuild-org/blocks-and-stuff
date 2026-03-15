package org.everbuild.blocksandstuff.recipes.smelting

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.recipes.api.BlockInventoryBackend
import org.everbuild.blocksandstuff.recipes.loader.FuelLoader

fun useFuel(inventory: BlockInventoryBackend): Int {
    if (inventory[1].isAir) return 0
    val fuelItem = FuelLoader.loadAllFuels().firstOrNull { it.itemStack.matches(inventory[1]) } ?: return 0
    val newFuelSlotItem =
        if (inventory[1].isSimilar(ItemStack.of(Material.LAVA_BUCKET)))
            ItemStack.of(Material.BUCKET)
        else {
            if (inventory[1].amount() == 1) ItemStack.AIR
            else inventory[1].withAmount(inventory[1].amount() - 1)
        }
    inventory.setItemStack(1, newFuelSlotItem)
    inventory.save()

    return fuelItem.burnTime
}

fun insertResult(inventory: BlockInventoryBackend, recipeData: SmeltingRecipeData) {
    val resultItem =
        if (inventory[2].isAir) recipeData.result else inventory[2].withAmount(inventory[2].amount() + recipeData.result.amount())
    val newInputItem =
        if (inventory[0].amount() == 1) ItemStack.AIR else inventory[0].withAmount(inventory[0].amount() - 1)
    inventory.setItemStack(0, newInputItem)
    inventory.setItemStack(2, resultItem)
    inventory.save()
}
