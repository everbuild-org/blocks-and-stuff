package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations

/** Shared item-ejection behaviour for droppers and dispensers. */
open class EjectorHandler(
    key: Key,
    archetype: ContainerArchetype,
) : StorageBlockHandler(key, archetype) {
    /** Removes and drops one random item from the inventory, if any. */
    internal open fun eject(
        instance: Instance,
        position: Point,
    ): ItemStack? {
        val inventory = backendOf(instance, position)
        val slot = randomFilledSlot(inventory) ?: return null
        val ejected = takeOneFrom(inventory, slot) ?: return null

        spawnItem(instance, position, instance.getBlock(position), ejected)
        RedstoneRegistrations.notifyContainerChanged(instance, position)
        return ejected
    }

    protected fun randomFilledSlot(inventory: SingleBlockInventoryBackend): Int? =
        inventory.extractableSlots.filter { !inventory.getItemStack(it).isAir }.randomOrNull()

    protected fun takeOneFrom(
        inventory: SingleBlockInventoryBackend,
        slot: Int,
    ): ItemStack? {
        val current = inventory.getItemStack(slot)
        if (current.isAir) return null

        inventory.transact { set ->
            set(slot, if (current.amount() > 1) current.withAmount(current.amount() - 1) else ItemStack.AIR)
        }
        return current.withAmount(1)
    }

    protected fun spawnItem(
        instance: Instance,
        position: Point,
        block: Block,
        stack: ItemStack,
    ) {
        spawnItemEntity(instance, position, facingOf(block), stack)
    }
}
