package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.util.toContainerItem
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventory
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHolder
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.blocksandstuff.redstone.VibrationBus

enum class ContainerAnimation {
    /** Client lid animation driven by a block action (chest, shulker box). */
    BLOCK_EVENT,

    /** Client lid animation driven by the `open` block property (barrel). */
    OPEN_PROPERTY,
}

class BlockContainerHandler(
    private val key: Key,
    private val archetype: ContainerArchetype,
    private val openSound: SoundEvent,
    private val closeSound: SoundEvent,
    private val animation: ContainerAnimation,
    private val keepContentsInItem: Boolean = false,
) : BlockHandler,
    BlockInventoryHolder {
    private var backend: SingleBlockInventoryBackend? = null
    private var listenersInstalled = false
    private var openCount = 0
    private var destroying = false

    override fun getKey(): Key = key

    override fun getInventory(
        instance: Instance,
        blockPos: Point,
    ): PhysicalInventory = backendOf(instance, blockPos)

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) return true

        val instance = interaction.instance
        val position = interaction.blockPosition
        val inventory = viewableInventory(instance, position)
        installAnimationListeners(inventory, instance, position)
        interaction.player.openInventory(inventory)
        return false
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        if (destroy.newBlock.key() == destroy.block.key()) return

        destroying = true
        if (keepContentsInItem) {
            backend
                ?.getViewableInventory()
                ?.viewers
                ?.toList()
                ?.forEach { it.closeInventory() }
            destroy.block.toContainerItem()?.let {
                DroppedItemFactory.maybeDrop(destroy.instance, destroy.blockPosition, it)
            }
            return
        }

        val destroyed = backendOf(destroy.instance, destroy.blockPosition).also { it.load(destroy.block) }
        BlockInventoryHandler.closeAndDrop(
            listOf(destroyed.getViewableInventory()),
            destroy.instance,
            destroy.blockPosition,
        )
    }

    private fun installAnimationListeners(
        inventory: BlockInventory,
        instance: Instance,
        position: Point,
    ) {
        if (listenersInstalled) return
        listenersInstalled = true

        inventory.eventNode().addListener(InventoryOpenEvent::class.java) { handleOpen(instance, position) }
        inventory.eventNode().addListener(InventoryCloseEvent::class.java) { handleClose(instance, position) }
        inventory.eventNode().addListener(InventoryItemChangeEvent::class.java) {
            RedstoneRegistrations.notifyContainerChanged(instance, position)
        }
    }

    private fun handleOpen(
        instance: Instance,
        position: Point,
    ) {
        openCount++
        VibrationBus.emit(instance, position, 10)
        playSound(instance, position, openSound)
        if (animation == ContainerAnimation.OPEN_PROPERTY) {
            setOpenProperty(instance, position, true)
        } else {
            instance.sendBlockAction(position, CHEST_LID_ACTION, openCount.toByte())
        }
    }

    private fun handleClose(
        instance: Instance,
        position: Point,
    ) {
        openCount = (openCount - 1).coerceAtLeast(0)
        if (destroying) return

        VibrationBus.emit(instance, position, 9)
        playSound(instance, position, closeSound)
        if (animation == ContainerAnimation.OPEN_PROPERTY) {
            if (openCount == 0) setOpenProperty(instance, position, false)
        } else {
            instance.sendBlockAction(position, CHEST_LID_ACTION, openCount.toByte())
        }
    }

    private fun setOpenProperty(
        instance: Instance,
        position: Point,
        open: Boolean,
    ) {
        val block = instance.getBlock(position)
        if (block.getProperty("open") == null) return
        instance.setBlock(position, block.withProperty("open", open.toString()), false)
    }

    private fun playSound(
        instance: Instance,
        position: Point,
        event: SoundEvent,
    ) {
        instance.playSoundExcept(null, Sound.sound(event, Sound.Source.BLOCK, 0.5f, 1.0f), position)
    }

    private fun viewableInventory(
        instance: Instance,
        position: Point,
    ): BlockInventory = backendOf(instance, position).getViewableInventory()

    private fun backendOf(
        instance: Instance,
        position: Point,
    ): SingleBlockInventoryBackend = backend ?: archetype.createBackend(instance, position).also { backend = it }

    private companion object {
        private const val CHEST_LID_ACTION: Byte = 1
    }
}
