package org.everbuild.blocksandstuff.recipes.grid

import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack

class CraftingTableGridService(private val inventory: Inventory) : AbstractCraftingGridService() {
    override val outputSlot: Int = 0
    override val width: Int = 3
    override val height: Int = 3

    override fun getPattern() = GridPattern.fromCraftingInventory(inventory)

    override fun setRecipeItemStack(
        index: Int,
        itemStack: ItemStack,
        sendUpdate: Boolean
    ) {
        inventory.setItemStack(index + 1, itemStack, sendUpdate)
    }

    override fun getRecipeItemStacks(): List<ItemStack> {
        return inventory.itemStacks.drop(1)
    }

    override fun setOutputItemStack(itemStack: ItemStack, sendUpdate: Boolean) {
        inventory.setItemStack(0, itemStack, sendUpdate)
    }

    override fun update() {
        inventory.update()
    }
}