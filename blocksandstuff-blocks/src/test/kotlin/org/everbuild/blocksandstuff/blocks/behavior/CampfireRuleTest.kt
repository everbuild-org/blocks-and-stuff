package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.recipe.RecipeBookCategory
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.recipes.RecipeFactory
import org.everbuild.blocksandstuff.recipes.serializer.ingredient.RecipeIngredient
import org.everbuild.blocksandstuff.recipes.serializer.ingredients.IngredientOrIngredients
import org.everbuild.blocksandstuff.recipes.smelting.campfire.CampfireCookingRecipe
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CampfireRuleTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler
    private val position = Pos(8.0, 64.0, 8.0)
    private val itemsTag: Tag<List<net.kyori.adventure.nbt.BinaryTag>> = Tag.NBT("Items").list()

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        MinecraftServer.getRecipeManager().addRecipe(
            CampfireCookingRecipe(
                IngredientOrIngredients.Ingredient(
                    RecipeIngredient.MaterialItem(Material.POTATO.key(), RecipeFactory.itemController),
                ),
                ItemStack.of(Material.BAKED_POTATO),
                0.35f,
                null,
                RecipeBookCategory.FURNACE_FOOD,
                5,
            ),
        )
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addCampfires() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:campfire"))
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun interact(
        block: Block,
        item: ItemStack,
        face: BlockFace = BlockFace.TOP,
    ): Boolean {
        instance.setBlock(position, block.withHandler(handler))
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.CREATIVE
                on { isSneaking } doReturn false
                on { getItemInHand(any()) } doReturn item
                on { itemInMainHand } doReturn item
            }
        return handler.onInteract(
            BlockHandler.Interaction(instance.getBlock(position), instance, face, position, Vec.ZERO, player, PlayerHand.MAIN),
        )
    }

    @Test
    fun lightsWithFlintAndSteel() {
        val handled = interact(Block.CAMPFIRE.withProperty("lit", "false"), ItemStack.of(Material.FLINT_AND_STEEL))

        Assertions.assertFalse(handled)
        Assertions.assertEquals("true", instance.getBlock(position).getProperty("lit"))
    }

    @Test
    fun shovelExtinguishesFromTop() {
        val handled = interact(Block.CAMPFIRE.withProperty("lit", "true"), ItemStack.of(Material.IRON_SHOVEL))

        Assertions.assertFalse(handled)
        Assertions.assertEquals("false", instance.getBlock(position).getProperty("lit"))
    }

    @Test
    fun shovelCannotExtinguishFromBottom() {
        val handled = interact(Block.CAMPFIRE.withProperty("lit", "true"), ItemStack.of(Material.IRON_SHOVEL), BlockFace.BOTTOM)

        Assertions.assertTrue(handled)
        Assertions.assertEquals("true", instance.getBlock(position).getProperty("lit"))
    }

    @Test
    fun addsFoodToCampfire() {
        val handled = interact(Block.CAMPFIRE.withProperty("lit", "true"), ItemStack.of(Material.POTATO))

        Assertions.assertFalse(handled)
        Assertions.assertNotNull(instance.getBlock(position).getTag(itemsTag))
    }

    @Test
    fun cookedFoodLeavesCampfire() {
        interact(Block.CAMPFIRE.withProperty("lit", "true"), ItemStack.of(Material.POTATO))
        repeat(5) {
            handler.tick(BlockHandler.Tick(instance.getBlock(position), instance, position))
        }

        Assertions.assertTrue(instance.getBlock(position).getTag(itemsTag).isNullOrEmpty())
    }
}
