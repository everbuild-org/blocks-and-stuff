package org.everbuild.blocksandstuff.recipes.smelting

import net.minestom.server.item.ItemStack

interface SmeltingRecipe {
    val result: ItemStack
    val burnTime: Int
    val experience: Float

    fun matches(itemStack: ItemStack): Boolean
}