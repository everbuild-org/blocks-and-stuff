package org.everbuild.blocksandstuff.recipes.stonecutting

import net.minestom.server.item.ItemStack

interface StonecuttingRecipe {
    val result: ItemStack

    fun matches(itemStack: ItemStack): Boolean
}