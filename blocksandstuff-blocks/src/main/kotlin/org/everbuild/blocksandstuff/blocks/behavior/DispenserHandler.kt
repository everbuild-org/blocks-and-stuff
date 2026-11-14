package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations

/** Dispenser: delegates items to [DispenseBehaviors], falling back to dropper-style ejection. */
class DispenserHandler(
    key: Key,
    archetype: ContainerArchetype,
) : EjectorHandler(key, archetype) {
    /** Dispenses one item from a random filled slot, if any. */
    internal fun dispense(
        instance: Instance,
        position: Point,
    ): ItemStack? {
        val inventory = backendOf(instance, position)
        val slot = randomFilledSlot(inventory) ?: return null
        val item = inventory.getItemStack(slot)
        val context =
            DispenseContext(
                instance,
                position,
                inventory,
                slot,
                item,
                facingOf(instance.getBlock(position)),
            )

        if (DispenseBehaviors.find(item)?.dispense(context) == true) return item.withAmount(1)

        val ejected = takeOneFrom(inventory, slot) ?: return null
        spawnItem(instance, position, instance.getBlock(position), ejected)
        RedstoneRegistrations.notifyContainerChanged(instance, position)
        return ejected
    }
}
