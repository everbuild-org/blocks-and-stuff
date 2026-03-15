package org.everbuild.blocksandstuff.recipes

import net.minestom.server.recipe.Recipe
import org.everbuild.blocksandstuff.recipes.api.StashController
import org.everbuild.blocksandstuff.recipes.impl.StashControllerImpl
import org.everbuild.blocksandstuff.recipes.grid.ShapedCraftingRecipe
import org.everbuild.blocksandstuff.recipes.grid.ShapelessCraftingRecipe
import org.everbuild.blocksandstuff.recipes.grid.TransmuteCraftingRecipe
import org.everbuild.blocksandstuff.recipes.loader.RecipeModel
import org.everbuild.blocksandstuff.recipes.smelting.blast_furnace.BlastFurnaceRecipe
import org.everbuild.blocksandstuff.recipes.smelting.furnace.FurnaceRecipe
import org.everbuild.blocksandstuff.recipes.smelting.smoker.SmokerRecipe
import org.everbuild.averium.org.everbuild.blocksandstuff.recipes.smithing.TransformSmithingRecipe
import org.everbuild.blocksandstuff.recipes.smithing.TrimSmithingRecipe
import org.everbuild.blocksandstuff.recipes.stonecutting.StonecutterRecipe
import org.everbuild.blocksandstuff.recipes.api.ItemController
import org.everbuild.blocksandstuff.recipes.impl.ItemControllerImpl

object RecipeFactory {
    var itemController: ItemController = ItemControllerImpl
    var stashController: StashController = StashControllerImpl

    fun create(model: RecipeModel): Recipe {
        return when(model) {
            is RecipeModel.ShapedCraftingRecipe -> ShapedCraftingRecipe(model)
            is RecipeModel.ShapelessCraftingRecipe -> ShapelessCraftingRecipe(model)
            is RecipeModel.TransmuteCraftingRecipe -> TransmuteCraftingRecipe(model)
            is RecipeModel.TransformSmithingRecipe -> TransformSmithingRecipe(model)
            is RecipeModel.TrimSmithingRecipe -> TrimSmithingRecipe(model)
            is RecipeModel.BlastingRecipe -> BlastFurnaceRecipe(model)
            is RecipeModel.SmeltingRecipe -> FurnaceRecipe(model)
            is RecipeModel.SmokingRecipe -> SmokerRecipe(model)
            is RecipeModel.StonecuttingRecipe -> StonecutterRecipe(model)
        }
    }
}