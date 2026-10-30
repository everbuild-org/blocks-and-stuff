package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventory
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryArchetype
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend

class ContainerArchetype(
    override val title: Component,
    override val inventoryType: InventoryType,
    override val size: Int,
) : BlockInventoryArchetype {
    override fun createInventory(backend: PhysicalInventory): BlockInventory = InventoryImpl(backend)

    fun createBackend(
        instance: Instance,
        blockPos: Point,
    ): SingleBlockInventoryBackend = Backend(blockPos, instance)

    inner class Backend(
        blockPos: Point,
        instance: Instance,
    ) : SingleBlockInventoryBackend(this@ContainerArchetype, blockPos, instance)

    inner class InventoryImpl(
        backend: PhysicalInventory,
    ) : BlockInventory(inventoryType, title, backend) {
        init {
            for (slot in 0 until size) {
                updateStackRaw(slot, backend.getItemStack(slot))
            }
        }
    }
}
