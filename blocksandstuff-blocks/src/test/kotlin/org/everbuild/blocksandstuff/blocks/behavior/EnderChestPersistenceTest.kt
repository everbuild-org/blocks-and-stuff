package org.everbuild.blocksandstuff.blocks.behavior

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.ContainerRegistrations
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

class EnderChestPersistenceTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 65.0, 8.0)

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            MinecraftServer.init()
            ContainerRegistrations.registerDefault()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            MinecraftServer.stopCleanly()
        }
    }

    @BeforeEach
    fun perTest() {
        EnderChestHandler.scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        EnderChestHandler.serverDispatcher = Dispatchers.Unconfined
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun reset() {
        EnderChestHandler.resetScheduling()
    }

    private class FakePersistence(
        private val items: List<ItemStack>,
    ) : EnderChestPersistence {
        var loaded = false
        var saved: List<ItemStack>? = null

        override suspend fun load(uuid: UUID): List<ItemStack> {
            loaded = true
            return items
        }

        override suspend fun save(
            uuid: UUID,
            items: List<ItemStack>,
        ) {
            saved = items
        }
    }

    private fun player(persistence: EnderChestPersistence?): Player =
        mock {
            on { uuid } doReturn UUID.randomUUID()
            on { isSneaking } doReturn false
            on { openInventory(any()) } doReturn true
            if (persistence != null) {
                on { getTag(ENDER_CHEST_PERSISTENCE) } doReturn persistence
            }
        }

    private fun open(
        handler: BlockHandler,
        player: Player,
    ): Inventory {
        val captor = argumentCaptor<Inventory>()
        handler.onInteract(
            BlockHandler.Interaction(Block.ENDER_CHEST, instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN),
        )
        verify(player, atLeast(1)).openInventory(captor.capture())
        return captor.lastValue
    }

    @Test
    fun loadsAndSavesThroughAttachedPersistence() {
        val persistence = FakePersistence(listOf(ItemStack.of(Material.DIAMOND, 2)))
        val player = player(persistence)
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:ender_chest"))

        val inventory = open(handler, player)
        Assertions.assertEquals(Material.DIAMOND, inventory.getItemStack(0).material())
        Assertions.assertEquals(2, inventory.getItemStack(0).amount())

        inventory.eventNode().call(InventoryCloseEvent(inventory, player, false))

        Assertions.assertTrue(persistence.loaded)
        Assertions.assertEquals(Material.DIAMOND, persistence.saved?.get(0)?.material())
        Assertions.assertEquals(2, persistence.saved?.get(0)?.amount())
    }

    @Test
    fun withoutPersistenceLoadsAndSavesNothing() {
        val persistence = FakePersistence(listOf(ItemStack.of(Material.DIAMOND, 2)))
        val player = player(null)
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:ender_chest"))

        val inventory = open(handler, player)
        Assertions.assertTrue(inventory.getItemStack(0).isAir)

        inventory.eventNode().call(InventoryCloseEvent(inventory, player, false))

        Assertions.assertFalse(persistence.loaded)
        Assertions.assertNull(persistence.saved)
    }

    @Test
    fun keepsContentsAcrossReopenWithoutPersistence() {
        val player = player(null)
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:ender_chest"))

        val inventory = open(handler, player)
        inventory.setItemStack(3, ItemStack.of(Material.STONE, 7))
        inventory.eventNode().call(InventoryCloseEvent(inventory, player, false))

        val reopened = open(handler, player)
        Assertions.assertEquals(Material.STONE, reopened.getItemStack(3).material())
        Assertions.assertEquals(7, reopened.getItemStack(3).amount())
    }
}
