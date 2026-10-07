package org.everbuild.blocksandstuff.recipes.smelting

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventory
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryArchetype
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.blockinventory.TagReader
import org.everbuild.blocksandstuff.recipes.initRecipeServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FurnaceInventoryTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() = initRecipeServer()
    }

    private object TestArchetype : FurnaceArchetype(Component.text("Furnace"), InventoryType.FURNACE)

    private class FakeBackend(
        override val archetype: BlockInventoryArchetype,
    ) : PhysicalInventory {
        val items = Array(archetype.size) { ItemStack.AIR }

        override fun getItemStack(slot: Int): ItemStack = items[slot]

        override fun transact(action: (setter: (slot: Int, itemStack: ItemStack?) -> Unit) -> Unit) {
            action { slot, itemStack -> items[slot] = itemStack ?: ItemStack.AIR }
        }

        override fun getViewableInventory(): BlockInventory = error("not needed for these tests")

        override fun readTags(): TagReader =
            object : TagReader {
                override fun <T> getTag(tag: Tag<T>): T = error("not needed for these tests")
            }
    }

    private fun furnace(): FurnaceArchetype.InventoryImpl {
        val archetype = TestArchetype
        return archetype.InventoryImpl(FakeBackend(archetype))
    }

    private fun player(vararg items: Pair<Int, ItemStack>): Pair<Player, PlayerInventory> {
        val playerInventory = PlayerInventory()
        items.forEach { (slot, item) -> playerInventory.setItemStack(slot, item) }
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)
        return player to playerInventory
    }

    @Test
    fun `picking up the output with the same item in the cursor is allowed`() {
        val inventory = furnace()
        inventory.setItemStack(FurnaceArchetype.SLOT_OUTPUT, ItemStack.of(Material.IRON_INGOT))

        val playerInventory = mock<PlayerInventory>()
        whenever(playerInventory.cursorItem).thenReturn(ItemStack.of(Material.IRON_INGOT))
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        val event = InventoryPreClickEvent(inventory, player, Click.Left(FurnaceArchetype.SLOT_OUTPUT))
        EventDispatcher.call(event)

        assertFalse(event.isCancelled, "a matching cursor stack must be able to take the output")
    }

    @Test
    fun `picking up the output with a different item in the cursor is blocked`() {
        val inventory = furnace()
        inventory.setItemStack(FurnaceArchetype.SLOT_OUTPUT, ItemStack.of(Material.IRON_INGOT))

        val playerInventory = mock<PlayerInventory>()
        whenever(playerInventory.cursorItem).thenReturn(ItemStack.of(Material.GOLD_INGOT))
        val player = mock<Player>()
        whenever(player.inventory).thenReturn(playerInventory)

        val event = InventoryPreClickEvent(inventory, player, Click.Left(FurnaceArchetype.SLOT_OUTPUT))
        EventDispatcher.call(event)

        assertTrue(event.isCancelled, "a mismatching cursor stack must not take the output")
    }

    @Test
    fun `shift click routes fuel into the fuel slot`() {
        val inventory = furnace()
        val (player, playerInventory) = player(0 to ItemStack.of(Material.COAL, 4))

        val handled = inventory.shiftClick(player, inventory.size + 0, 0)

        assertTrue(handled)
        assertEquals(Material.COAL, inventory.getItemStack(FurnaceArchetype.SLOT_FUEL).material())
        assertEquals(4, inventory.getItemStack(FurnaceArchetype.SLOT_FUEL).amount())
        assertTrue(playerInventory.getItemStack(0).isAir)
    }

    @Test
    fun `shift click routes smeltable items into the input slot`() {
        val inventory = furnace()
        val (player, playerInventory) = player(0 to ItemStack.of(Material.RAW_IRON, 3))

        val handled = inventory.shiftClick(player, inventory.size + 0, 0)

        assertTrue(handled)
        assertEquals(Material.RAW_IRON, inventory.getItemStack(FurnaceArchetype.SLOT_INPUT).material())
        assertEquals(3, inventory.getItemStack(FurnaceArchetype.SLOT_INPUT).amount())
        assertTrue(playerInventory.getItemStack(0).isAir)
    }

    @Test
    fun `shift click moves the output into the player inventory`() {
        val inventory = furnace()
        inventory.setItemStack(FurnaceArchetype.SLOT_OUTPUT, ItemStack.of(Material.IRON_INGOT, 2))
        val (player, playerInventory) = player()

        val handled = inventory.shiftClick(player, FurnaceArchetype.SLOT_OUTPUT, 0)

        assertTrue(handled)
        assertTrue(inventory.getItemStack(FurnaceArchetype.SLOT_OUTPUT).isAir)
        assertEquals(2, playerInventory.itemStacks.sumOf { it.amount() })
    }
}
