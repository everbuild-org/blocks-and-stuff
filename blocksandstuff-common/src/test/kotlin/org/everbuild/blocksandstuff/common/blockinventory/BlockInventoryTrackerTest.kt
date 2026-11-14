package org.everbuild.blocksandstuff.common.blockinventory

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BlockInventoryTrackerTest {
    private lateinit var firstInstance: Instance
    private lateinit var secondInstance: Instance
    private var previousDropSetting = true
    private val position = Pos(5.0, 64.0, 5.0)

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        firstInstance = MinecraftServer.getInstanceManager().createInstanceContainer()
        secondInstance = MinecraftServer.getInstanceManager().createInstanceContainer()
        previousDropSetting = DroppedItemFactory.doDropItems
        DroppedItemFactory.doDropItems = false
    }

    @AfterEach
    fun tearDown() {
        DroppedItemFactory.doDropItems = previousDropSetting
        MinecraftServer.stopCleanly()
    }

    @Test
    fun destroyingInventoryInOneInstanceLeavesSamePositionInventoryAlone() {
        val firstInventory = trackedInventory()
        val secondInventory = trackedInventory()
        firstInventory.setItemStack(0, ItemStack.of(Material.STONE))
        secondInventory.setItemStack(0, ItemStack.of(Material.DIAMOND))
        BlockInventoryTracker.track(firstInventory, firstInstance, position)
        BlockInventoryTracker.track(secondInventory, secondInstance, position)

        BlockInventoryTracker.onDestroy(firstInstance, position, Block.CRAFTING_TABLE, Block.AIR)

        assertTrue(firstInventory.getItemStack(0).isAir)
        assertEquals(Material.DIAMOND, secondInventory.getItemStack(0).material())
    }

    private fun trackedInventory(): Inventory =
        object : Inventory(InventoryType.WINDOW_3X3, Component.text("Tracked")), BlockInventoryDrops {
            override val dropSlots: Collection<Int> = listOf(0)
        }
}
