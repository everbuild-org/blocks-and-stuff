package org.everbuild.blocksandstuff.recipes.stonecutting

import net.minestom.server.item.ItemStack
import net.minestom.server.recipe.display.SlotDisplay

interface StonecuttingRecipe {
    val result: ItemStack

    /**
     * Registration order. Minestom sends the stonecutter recipe list to the client in the order the
     * recipes were registered (display id order), and the client sends back the index into that list,
     * so selection must be resolved using the same order.
     */
    val order: Int

    fun matches(itemStack: ItemStack): Boolean

    // <@AI_UNREVIEWED>
    /**
     * The ingredient displayed to the client. Only [SlotDisplay.Item] and [SlotDisplay.Tag] are
     * converted by Minestom when building the stonecutter recipe list, every other display type is
     * silently dropped. The server must apply the same filter so button indices stay aligned.
     */
    fun ingredientDisplay(): SlotDisplay
    // </<@AI_UNREVIEWED>
}