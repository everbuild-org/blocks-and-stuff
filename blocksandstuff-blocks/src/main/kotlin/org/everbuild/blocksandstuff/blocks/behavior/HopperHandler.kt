package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.ItemEntity
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHolder
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend
import org.everbuild.blocksandstuff.common.blockinventory.extractOne
import org.everbuild.blocksandstuff.common.blockinventory.insertStack
import org.everbuild.blocksandstuff.common.utils.handlerOrRegistered
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations

/**
 * Vanilla-like hopper: five inventory slots, pushes one item into the container it faces, pulls
 * from the container above (or collects item entities), and pauses while powered by redstone.
 */
class HopperHandler(
    private val key: Key,
    private val archetype: ContainerArchetype,
) : BlockInventoryHandler(),
    BlockInventoryHolder {
    private var backend: SingleBlockInventoryBackend? = null
    private var cooldown = 0
    private var changeListenerInstalled = false

    override fun getKey(): Key = key

    override fun isTickable(): Boolean = true

    override fun getInventory(
        instance: Instance,
        blockPos: Point,
    ): PhysicalInventory = backendOf(instance, blockPos)

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) return true

        val inventory = viewableInventory(interaction.instance, interaction.blockPosition)
        if (!changeListenerInstalled) {
            changeListenerInstalled = true
            inventory.eventNode().addListener(InventoryItemChangeEvent::class.java) {
                RedstoneRegistrations.notifyContainerChanged(interaction.instance, interaction.blockPosition)
            }
        }
        interaction.player.openInventory(inventory)
        return false
    }

    override fun tick(tick: BlockHandler.Tick) {
        if (tick.block.key() != key) return

        if (cooldown > 0) {
            cooldown--
            if (cooldown > 0) return
        }
        if (isPowered(tick)) return

        val own = backendOf(tick.instance, tick.blockPosition)
        val targetPosition = tick.blockPosition.relative(facingOf(tick.block))
        val target = containerAt(tick.instance, targetPosition)
        val sourcePosition = tick.blockPosition.add(0.0, 1.0, 0.0)
        val source = containerAt(tick.instance, sourcePosition)

        val targetWasEmpty = target?.inventory?.let { isEmpty(it) } ?: false
        var moved = target != null && pushOne(own, target.inventory)
        moved =
            if (source != null) {
                pullOne(source.inventory, own) || moved
            } else {
                collectItems(tick.instance, tick.blockPosition, own) || moved
            }
        if (!moved) return

        if (targetWasEmpty) (target.handler as? HopperHandler)?.receiveItem()
        cooldown = TRANSFER_COOLDOWN
        RedstoneRegistrations.notifyContainerChanged(tick.instance, tick.blockPosition)
        if (target != null) RedstoneRegistrations.notifyContainerChanged(tick.instance, targetPosition)
        if (source != null) RedstoneRegistrations.notifyContainerChanged(tick.instance, sourcePosition)
    }

    internal fun receiveItem() {
        cooldown = TRANSFER_COOLDOWN
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        if (destroy.newBlock.key() == destroy.block.key()) return

        val destroyed = backendOf(destroy.instance, destroy.blockPosition).also { it.load(destroy.block) }
        closeAndDrop(
            listOf(destroyed.getViewableInventory()),
            destroy.instance,
            destroy.blockPosition,
        )
        backend = null
        cooldown = 0
    }

    private fun pushOne(
        source: PhysicalInventory,
        target: PhysicalInventory,
    ): Boolean {
        for (slot in source.extractableSlots) {
            val stack = source.getItemStack(slot)
            if (stack.isAir) continue
            if (!target.insertStack(stack.withAmount(1)).isAir) continue

            source.transact { set ->
                val current = source.getItemStack(slot)
                set(slot, if (current.amount() > 1) current.withAmount(current.amount() - 1) else null)
            }
            return true
        }
        return false
    }

    private fun pullOne(
        source: PhysicalInventory,
        target: PhysicalInventory,
    ): Boolean {
        val extracted = source.extractOne() ?: return false
        val leftover = target.insertStack(extracted)
        if (!leftover.isAir) {
            source.insertStack(leftover)
            return false
        }
        return true
    }

    private fun collectItems(
        instance: Instance,
        position: Point,
        target: PhysicalInventory,
    ): Boolean {
        val center = position.add(0.5, 1.0, 0.5)
        var moved = false

        for (entity in instance.getNearbyEntities(center, PICKUP_RANGE).filterIsInstance<ItemEntity>()) {
            if (!entity.isPickable) continue
            if (!isInPickupArea(entity.position, position)) continue

            val stack = entity.itemStack
            if (stack.isAir) continue

            val leftover = target.insertStack(stack)
            if (leftover.amount() == stack.amount()) continue

            moved = true
            if (leftover.isAir) entity.remove() else entity.setItemStack(leftover)
        }
        return moved
    }

    private fun isInPickupArea(
        point: Point,
        hopperPosition: Point,
    ): Boolean =
        point.x() >= hopperPosition.x() &&
            point.x() < hopperPosition.x() + 1.0 &&
            point.z() >= hopperPosition.z() &&
            point.z() < hopperPosition.z() + 1.0 &&
            point.y() >= hopperPosition.y() + 1.0 &&
            point.y() < hopperPosition.y() + 2.0

    private fun isPowered(tick: BlockHandler.Tick): Boolean =
        RedstoneRegistrations.simulator(tick.instance)?.isPowered(tick.blockPosition) == true

    private fun containerAt(
        instance: Instance,
        position: Point,
    ): ContainerRef? {
        val block = instance.getBlock(position)
        if (block.air()) return null

        val holder = block.handlerOrRegistered() as? BlockInventoryHolder ?: return null
        return ContainerRef(holder, holder.getInventory(instance, position))
    }

    private fun isEmpty(inventory: PhysicalInventory): Boolean = inventory.extractableSlots.all { inventory.getItemStack(it).isAir }

    private data class ContainerRef(
        val handler: BlockInventoryHolder,
        val inventory: PhysicalInventory,
    )

    private fun facingOf(block: Block): BlockFace =
        when (block.getProperty("facing")) {
            "north" -> BlockFace.NORTH
            "south" -> BlockFace.SOUTH
            "east" -> BlockFace.EAST
            "west" -> BlockFace.WEST
            else -> BlockFace.BOTTOM
        }

    private fun viewableInventory(
        instance: Instance,
        position: Point,
    ) = backendOf(instance, position).getViewableInventory()

    private fun backendOf(
        instance: Instance,
        position: Point,
    ): SingleBlockInventoryBackend = backend ?: archetype.createBackend(instance, position).also { backend = it }

    private companion object {
        private const val TRANSFER_COOLDOWN = 8
        private const val PICKUP_RANGE = 2.0
    }
}
