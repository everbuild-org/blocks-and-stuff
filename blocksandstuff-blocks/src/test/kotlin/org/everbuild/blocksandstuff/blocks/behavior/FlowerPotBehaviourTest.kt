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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class FlowerPotBehaviourTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        CustomPlants.resetDefaults()
    }

    @AfterEach
    fun teardown() {
        CustomPlants.resetDefaults()
        MinecraftServer.stopCleanly()
    }

    private fun interact(
        block: Block,
        item: ItemStack,
    ): Boolean {
        val handler = FlowerPotHandler(block.key())
        val placed = block.withHandler(handler)
        instance.setBlock(position, placed)
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.SURVIVAL
                on { isSneaking } doReturn false
                on { getItemInHand(any()) } doReturn item
            }
        return handler.onInteract(
            BlockHandler.Interaction(placed, instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN),
        )
    }

    @Test
    fun placesVanillaPlantInEmptyPot() {
        val handled = interact(Block.FLOWER_POT, ItemStack.of(Material.POPPY))

        Assertions.assertFalse(handled)
        Assertions.assertEquals(Block.POTTED_POPPY.key(), instance.getBlock(position).key())
    }

    @Test
    fun removesPlantFromPot() {
        val handled = interact(Block.POTTED_POPPY, ItemStack.AIR)

        Assertions.assertFalse(handled)
        Assertions.assertEquals(Block.FLOWER_POT.key(), instance.getBlock(position).key())
    }

    @Test
    fun ignoresUnregisteredItems() {
        val handled = interact(Block.FLOWER_POT, ItemStack.of(Material.DIAMOND))

        Assertions.assertTrue(handled)
        Assertions.assertEquals(Block.FLOWER_POT.key(), instance.getBlock(position).key())
    }

    @Test
    fun supportsCustomPlants() {
        CustomPlants.register(Material.DIAMOND, Block.POTTED_ALLIUM, ItemStack.of(Material.DIAMOND))

        val handled = interact(Block.FLOWER_POT, ItemStack.of(Material.DIAMOND))

        Assertions.assertFalse(handled)
        Assertions.assertEquals(Block.POTTED_ALLIUM.key(), instance.getBlock(position).key())
        Assertions.assertEquals(Material.DIAMOND, CustomPlants.findForBlock(Block.POTTED_ALLIUM)?.drop?.material())
    }
}
