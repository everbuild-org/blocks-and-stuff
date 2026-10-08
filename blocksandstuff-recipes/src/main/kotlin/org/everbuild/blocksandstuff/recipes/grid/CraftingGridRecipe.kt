package org.everbuild.blocksandstuff.recipes.grid

import net.minestom.server.item.ItemStack

interface CraftingGridRecipe {
    fun getResult(source: GridPattern<ItemStack>): ItemStack
    fun matches(pattern: GridPattern<ItemStack>): Boolean
    fun takeOne(pattern: GridPattern<ItemStack>): GridPattern<ItemStack>?
}