package org.everbuild.blocksandstuff.recipes.smelting

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.recipes.loader.FuelLoader
import org.everbuild.blocksandstuff.recipes.smelting.FurnaceArchetype.Companion.SLOT_FUEL
import org.everbuild.blocksandstuff.recipes.smelting.FurnaceArchetype.Companion.SLOT_INPUT
import org.everbuild.blocksandstuff.recipes.smelting.FurnaceArchetype.Companion.SLOT_OUTPUT

fun useFuel(inventory: PhysicalInventory): Int {
    if (inventory[SLOT_FUEL].isAir) return 0
    val fuelItem = FuelLoader.loadAllFuels().firstOrNull { it.itemStack.matches(inventory[SLOT_FUEL]) } ?: return 0
    val newFuelSlotItem =
        if (inventory[SLOT_FUEL].isSimilar(ItemStack.of(Material.LAVA_BUCKET))) {
            ItemStack.of(Material.BUCKET)
        } else {
            if (inventory[SLOT_FUEL].amount() == 1) {
                ItemStack.AIR
            } else {
                inventory[SLOT_FUEL].let { it.withAmount(it.amount() - 1) }
            }
        }
    inventory.transact { setter -> setter(SLOT_FUEL, newFuelSlotItem) }

    return fuelItem.burnTime
}

fun insertResult(
    inventory: PhysicalInventory,
    recipeData: SmeltingRecipeData,
) {
    val outputSlotItem = inventory[SLOT_OUTPUT]
    val resultItem =
        if (outputSlotItem.isAir) {
            recipeData.result
        } else {
            outputSlotItem.withAmount(outputSlotItem.amount() + recipeData.result.amount())
        }

    val inputSlotItem = inventory[SLOT_INPUT]
    val newInputItem =
        if (inputSlotItem.amount() <= 1) {
            ItemStack.AIR
        } else {
            inputSlotItem.withAmount(inputSlotItem.amount() - 1)
        }

    inventory.transact { setter ->
        setter(SLOT_INPUT, newInputItem)
        setter(SLOT_OUTPUT, resultItem)
    }
}
