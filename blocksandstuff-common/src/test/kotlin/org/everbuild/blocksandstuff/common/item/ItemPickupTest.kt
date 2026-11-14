package org.everbuild.blocksandstuff.common.item

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ItemPickupTest {
    private lateinit var instance: Instance

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            MinecraftServer.init()
            DroppedItemFactory.enableItemPickup()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            MinecraftServer.stopCleanly()
        }
    }

    @BeforeEach
    fun perTestSetup() {
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    private fun player(inventory: PlayerInventory): Player =
        mock {
            on { getInventory() } doReturn inventory
            on { getInstance() } doReturn this@ItemPickupTest.instance
        }

    private fun pick(
        player: Player,
        amount: Int,
    ): PickupItemEvent {
        val entity = ItemEntity(ItemStack.of(Material.STONE, amount))
        entity.setInstance(instance, Pos(0.0, 64.0, 0.0))
        val event = PickupItemEvent(player, entity)
        EventDispatcher.call(event)
        return event
    }

    @Test
    fun pickupWhenSpaceFits() {
        val inventory = PlayerInventory()
        val event = pick(player(inventory), 5)

        Assertions.assertFalse(event.isCancelled)
        Assertions.assertEquals(5, (0 until inventory.size).sumOf { inventory.getItemStack(it).amount() })
    }

    @Test
    fun pickupWhenNothingFitsIsCancelled() {
        val inventory = PlayerInventory()
        for (slot in 0 until inventory.innerSize) {
            inventory.setItemStack(slot, ItemStack.of(Material.DIRT, 64))
        }
        val event = pick(player(inventory), 5)

        Assertions.assertTrue(event.isCancelled)
    }

    @Test
    fun partialPickupFillsExistingStacks() {
        val inventory = PlayerInventory()
        for (slot in 0 until inventory.innerSize) {
            inventory.setItemStack(slot, ItemStack.of(Material.DIRT, 64))
        }
        inventory.setItemStack(0, ItemStack.of(Material.STONE, 60))
        val event = pick(player(inventory), 10)

        Assertions.assertFalse(event.isCancelled)
        Assertions.assertEquals(64, inventory.getItemStack(0).amount())
    }
}
