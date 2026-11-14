package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventory
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHolder
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations

/** Block-backed storage without a player-facing GUI (dropper, dispenser). */
open class StorageBlockHandler(
    private val key: Key,
    private val archetype: ContainerArchetype,
) : BlockInventoryHandler(),
    BlockInventoryHolder {
    private var backend: SingleBlockInventoryBackend? = null
    private var changeListenerInstalled = false

    override fun getKey(): Key = key

    override fun getInventory(
        instance: Instance,
        blockPos: Point,
    ): PhysicalInventory = backendOf(instance, blockPos)

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) return true

        interaction.player.openInventory(viewableInventory(interaction.instance, interaction.blockPosition))
        return false
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        if (destroy.newBlock.key() == destroy.block.key()) return

        val destroyed = backendOf(destroy.instance, destroy.blockPosition).also { it.load(destroy.block) }
        BlockInventoryHandler.closeAndDrop(
            listOf(destroyed.getViewableInventory()),
            destroy.instance,
            destroy.blockPosition,
        )
        backend = null
    }

    private fun viewableInventory(
        instance: Instance,
        position: Point,
    ): BlockInventory {
        val inventory = backendOf(instance, position).getViewableInventory()
        if (!changeListenerInstalled) {
            changeListenerInstalled = true
            inventory.eventNode().addListener(InventoryItemChangeEvent::class.java) {
                RedstoneRegistrations.notifyContainerChanged(instance, position)
            }
        }
        return inventory
    }

    protected fun backendOf(
        instance: Instance,
        position: Point,
    ): SingleBlockInventoryBackend = backend ?: archetype.createBackend(instance, position).also { backend = it }
}
