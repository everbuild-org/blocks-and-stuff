package org.everbuild.blocksandstuff.recipes.smelting

import net.minestom.server.item.ItemStack

data class SmeltingRecipeData(
    val result: ItemStack,
    val experience: Float,
    val burnTime: Int,
)
