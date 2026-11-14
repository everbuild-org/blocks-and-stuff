package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.ContainerRegistrations
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class BlockContainerHandlerTest {
    private lateinit var instance: Instance
    private val archetype = ContainerArchetype(Component.text("Chest"), InventoryType.CHEST_3_ROW, 27)
    private val position = Pos(8.0, 65.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        ContainerRegistrations.registerDefault()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun placeChest(block: Block = Block.CHEST) {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler(block.key().asString()))
        instance.setBlock(position, block.withHandler(handler))
    }

    @Test
    fun persistedItemsSurviveBackendRecreation() {
        placeChest()

        val first = archetype.Backend(position, instance)
        first.getViewableInventory().setItemStack(0, ItemStack.of(Material.STONE, 3))

        val second = archetype.Backend(position, instance)
        Assertions.assertEquals(Material.STONE, second.getItemStack(0).material())
        Assertions.assertEquals(3, second.getItemStack(0).amount())
    }

    @Test
    fun interactOpensInventoryWithPersistedItems() {
        placeChest()
        archetype
            .Backend(position, instance)
            .getViewableInventory()
            .setItemStack(1, ItemStack.of(Material.DIAMOND, 5))

        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:chest"))
        val player =
            mock<Player> {
                on { isSneaking } doReturn false
                on { openInventory(any()) } doReturn true
            }

        val handled =
            handler.onInteract(
                BlockHandler.Interaction(Block.CHEST, instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN),
            )

        Assertions.assertFalse(handled)
        val captor = argumentCaptor<Inventory>()
        verify(player).openInventory(captor.capture())
        Assertions.assertEquals(Material.DIAMOND, captor.firstValue.getItemStack(1).material())
        Assertions.assertEquals(5, captor.firstValue.getItemStack(1).amount())
    }

    @Test
    fun sneakingDoesNotOpenInventory() {
        placeChest()

        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:chest"))
        val player =
            mock<Player> {
                on { isSneaking } doReturn true
            }

        val handled =
            handler.onInteract(
                BlockHandler.Interaction(Block.CHEST, instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN),
            )

        Assertions.assertTrue(handled)
        verify(player, never()).openInventory(any())
    }

    @Test
    fun barrelOpenPropertyFollowsViewers() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:barrel"))
        instance.setBlock(position, Block.BARREL.withHandler(handler))

        val player =
            mock<Player> {
                on { isSneaking } doReturn false
            }
        val captor = argumentCaptor<Inventory>()
        handler.onInteract(
            BlockHandler.Interaction(Block.BARREL, instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN),
        )
        verify(player).openInventory(captor.capture())
        val inventory = captor.firstValue

        inventory.eventNode().call(InventoryOpenEvent(inventory, player))
        Assertions.assertEquals("true", instance.getBlock(position).getProperty("open"))

        inventory.eventNode().call(InventoryCloseEvent(inventory, player, false))
        Assertions.assertEquals("false", instance.getBlock(position).getProperty("open"))
    }
}
