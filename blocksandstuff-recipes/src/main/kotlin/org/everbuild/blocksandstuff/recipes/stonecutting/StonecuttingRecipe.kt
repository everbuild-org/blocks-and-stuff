package org.everbuild.blocksandstuff.recipes.stonecutting

import net.minestom.server.item.ItemStack

interface StonecuttingRecipe {
    val result: ItemStack

    /**
     * Registration order. Minestom sends the stonecutter recipe list to the client in the order the
     * recipes were registered (display id order), and the client sends back the index into that list,
     * so selection must be resolved using the same order.
     */
    val order: Int

    fun matches(itemStack: ItemStack): Boolean
}