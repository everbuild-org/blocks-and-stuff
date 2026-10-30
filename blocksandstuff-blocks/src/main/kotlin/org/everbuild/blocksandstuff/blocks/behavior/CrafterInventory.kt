package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.network.packet.server.play.SetSlotPacket
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventory
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryArchetype
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend
import org.everbuild.blocksandstuff.recipes.grid.CraftingGridRecipe
import org.everbuild.blocksandstuff.recipes.grid.GridPattern
import org.everbuild.blocksandstuff.recipes.grid.filterInvalid

internal object CrafterArchetype : BlockInventoryArchetype {
    override val title: Component = Component.translatable("container.crafter")
    override val inventoryType: InventoryType = InventoryType.CRAFTER_3X3
    override val size: Int = 9

    override fun createInventory(backend: PhysicalInventory): BlockInventory = CrafterInventory(backend)

    fun createBackend(
        instance: Instance,
        position: Point,
    ): SingleBlockInventoryBackend = CrafterBackend(this, position, instance)

    private class CrafterBackend(
        archetype: BlockInventoryArchetype,
        position: Point,
        instance: Instance,
    ) : SingleBlockInventoryBackend(archetype, position, instance)
}

internal class CrafterInventory(
    backend: PhysicalInventory,
) : BlockInventory(InventoryType.CRAFTER_3X3, CrafterArchetype.title, backend) {
    init {
        for (slot in 0 until 9) {
            setItemStack(slot, backend.getItemStack(slot), false)
        }
        eventNode().addListener(InventoryItemChangeEvent::class.java) { event ->
            if (event.slot in 0 until 9) refreshPreview()
        }
        eventNode().addListener(InventoryPreClickEvent::class.java) { event ->
            val slot = event.slot
            if (slot in 0 until 9 && getItemStack(slot).isAir && event.player.inventory.cursorItem.isAir) {
                CrafterSessions.toggle(event.player, slot)
                event.isCancelled = true
            }
        }
        refreshPreview()
    }

    fun refreshPreview() {
        val pattern =
            GridPattern(
                List(3) { row ->
                    List(3) { column -> getItemStack(row * 3 + column) }
                },
                nullValue = ItemStack.AIR,
            ).filterInvalid().minimizePattern()
        val recipe =
            MinecraftServer
                .getRecipeManager()
                .recipes
                .filterIsInstance<CraftingGridRecipe>()
                .firstOrNull { it.matches(pattern) }
        val result = recipe?.getResult(pattern) ?: ItemStack.AIR
        val packet = SetSlotPacket(windowId.toInt(), 0, RESULT_SLOT.toShort(), result)
        viewers.forEach { it.sendPacket(packet) }
    }

    private companion object {
        /** Crafter menu slot after the 9 inputs and the player's 36 inventory slots. */
        private const val RESULT_SLOT = 45
    }
}
