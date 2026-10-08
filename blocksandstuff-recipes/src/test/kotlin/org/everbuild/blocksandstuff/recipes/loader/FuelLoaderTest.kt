package org.everbuild.blocksandstuff.recipes.loader

import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.recipes.ingredient
import org.everbuild.blocksandstuff.recipes.initRecipeServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FuelLoaderTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = initRecipeServer()

        @JvmStatic
        @AfterAll
        fun teardown() {
            FuelLoader.furnaceFuels = null
        }
    }

    @Test
    fun `vanilla fuels are recognised through the cooking_fuel component`() {
        assertTrue(FuelLoader.isFuel(ItemStack.of(Material.COAL)))
    }

    @Test
    fun `non fuels are not recognised`() {
        assertTrue(!FuelLoader.isFuel(ItemStack.of(Material.DIRT)))
    }

    @Test
    fun `burn time falls back to the fuel list for provider-backed vanilla fuels`() {
        FuelLoader.furnaceFuels = listOf(FurnaceFuel(ingredient(Material.COAL), 1600))
        try {
            assertEquals(1600, FuelLoader.burnTime(ItemStack.of(Material.COAL)))
        } finally {
            FuelLoader.furnaceFuels = null
        }
    }

    @Test
    fun `burn time is unknown without the fuel list`() {
        FuelLoader.furnaceFuels = null
        assertNull(FuelLoader.burnTime(ItemStack.of(Material.COAL)))
    }
}
