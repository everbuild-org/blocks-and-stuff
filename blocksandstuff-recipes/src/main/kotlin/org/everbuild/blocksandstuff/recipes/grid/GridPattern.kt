package org.everbuild.blocksandstuff.recipes.grid

import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.recipes.RecipeFactory
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients

data class GridPattern<T>(val grid: List<List<T?>>, val minRow: Int = 0, val minCol: Int = 0, val nullValue: T) {
    val width: Int = grid.getOrNull(0)?.size ?: 0
    val height: Int = grid.size

    init {
        require(grid.all { it.size == grid[0].size }) { "Grid must be square (all rows must have the same length)" }
    }

    val ingredients: List<T>
        get() = grid.flatten().mapNotNull { it ?: nullValue }

    fun minimizePattern(): GridPattern<T> {
        if (grid.isEmpty()) return this

        var minRow = grid.size
        var maxRow = -1
        var minCol = grid[0].size
        var maxCol = -1

        for (i in grid.indices) {
            for (j in grid[i].indices) {
                if (grid[i][j] != IngredientOrIngredients.air(RecipeFactory.itemController) && grid[i][j] != ItemStack.AIR && grid[i][j] != null) {
                    minRow = minRow.coerceAtMost(i)
                    maxRow = maxRow.coerceAtLeast(i)
                    minCol = minCol.coerceAtMost(j)
                    maxCol = maxCol.coerceAtLeast(j)
                }
            }
        }

        if (maxRow == -1 || maxCol == -1) return GridPattern(listOf(), nullValue = nullValue)

        return GridPattern(
            grid.subList(minRow, maxRow + 1).map { row ->
                row.subList(minCol, maxCol + 1)
            },
            minRow,
            minCol,
            nullValue
        )
    }

    fun extendPattern(width: Int, height: Int): GridPattern<T> {
        if (grid.isEmpty()) {
            return GridPattern(List(height) { List(width) { null } }, nullValue = nullValue)
        }
        val right = width - grid[0].size - minCol
        val bottom = height - grid.size - minRow

        return GridPattern(
            List(height) { y ->
                if (y < minRow || height - y <= bottom) List(width) { null }
                else List(width) { x ->
                    if (x < minCol || width - x <= right) null
                    else grid[y - minRow][x - minCol]
                }
            },
            0, 0,
            nullValue
        )
    }

    companion object {
        fun fromCraftingInventory(inventory: Inventory): GridPattern<ItemStack> {
            assert(inventory.inventoryType == InventoryType.CRAFTING)
            return GridPattern(
                inventory.itemStacks
                    .asSequence()
                    .drop(1)
                    .take(9)
                    .toList()
                    .windowed(3, 3, false),
                nullValue = ItemStack.AIR
            )
        }

        fun fromPlayerInventoryGrid(inventory: PlayerInventory): GridPattern<ItemStack> {
            return GridPattern(
                inventory.itemStacks
                    .drop(37)
                    .take(4)
                    .windowed(2, 2, false),
                nullValue = ItemStack.AIR
            )
        }

        fun fromRecipeDefinition(
            pattern: List<String>,
            key: Map<String, IngredientOrIngredients>
        ): GridPattern<IngredientOrIngredients> {
            return GridPattern(
                pattern.map { ingredients ->
                    ingredients.split("")
                        .map { ingredient -> ingredient.trim() }
                        .map { ingredient -> key[ingredient] }
                        .mapNotNull { it ?: IngredientOrIngredients.air(RecipeFactory.itemController) }
                },
                nullValue = IngredientOrIngredients.air(RecipeFactory.itemController)

            )
        }
    }

    override fun toString(): String {
        if (grid.isEmpty()) return "${width}x$height\nempty"

        val chars = ('A'..'Z').iterator()
        val mapping = mutableMapOf<T, Char>()

        val pattern = grid.joinToString("\n") { row ->
            row.joinToString("") { item ->
                when (item) {
                    null -> " "
                    else -> mapping.getOrPut(item) { chars.next() }.toString()
                }
            }
        }

        val materials = mapping.entries.joinToString("\n") { (ingredient, char) ->
            "$char -> $ingredient"
        }

        return "${width}x$height\n$pattern\n\n$materials"
    }
}

fun GridPattern<IngredientOrIngredients>.matches(pattern: GridPattern<ItemStack>): Boolean {
    if (pattern.width != width || pattern.height != height) return false
    return pattern.grid
        .flatten()
        .zip(grid.flatten())
        .all { (pattern, field) ->
            if (pattern == null || field == null) return@all pattern == field
            return@all field.matches(pattern)
        }
}

fun GridPattern<ItemStack>.filterInvalid(): GridPattern<ItemStack> {
    // filter all air with a different amount than one
    return GridPattern(
        grid.map { row -> row.map { if (it?.isSimilar(ItemStack.AIR) ?: true) ItemStack.AIR else it } },
        minRow, minCol,
        nullValue
    )
}
