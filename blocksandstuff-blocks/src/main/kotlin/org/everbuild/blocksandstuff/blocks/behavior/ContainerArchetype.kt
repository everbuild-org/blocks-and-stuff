package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventory
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryArchetype
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend

class ContainerArchetype(
    override val title: Component,
    override val inventoryType: InventoryType,
    override val size: Int,
    private val acceptsShulkerBoxes: Boolean = true,
) : BlockInventoryArchetype {
    override fun createInventory(backend: PhysicalInventory): BlockInventory = InventoryImpl(backend)

    fun createBackend(
        instance: Instance,
        blockPos: Point,
    ): SingleBlockInventoryBackend = Backend(blockPos, instance)

    inner class Backend(
        blockPos: Point,
        instance: Instance,
    ) : SingleBlockInventoryBackend(this@ContainerArchetype, blockPos, instance) {
        override fun canPlaceItem(
            slot: Int,
            stack: ItemStack,
        ): Boolean {
            if (acceptsShulkerBoxes || stack.isAir) return true
            val key = stack.material().key().value()
            return key != "shulker_box" && !key.endsWith("_shulker_box")
        }
    }

    inner class InventoryImpl(
        backend: PhysicalInventory,
    ) : BlockInventory(inventoryType, title, backend) {
        init {
            rejectsShulkerBoxes = !acceptsShulkerBoxes
            for (slot in 0 until size) {
                updateStackRaw(slot, backend.getItemStack(slot))
            }
        }
    }
}
