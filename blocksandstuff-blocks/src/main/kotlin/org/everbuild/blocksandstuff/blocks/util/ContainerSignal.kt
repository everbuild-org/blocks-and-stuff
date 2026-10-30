package org.everbuild.blocksandstuff.blocks.util

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHolder
import org.everbuild.blocksandstuff.common.blockinventory.comparatorSignal
import org.everbuild.blocksandstuff.common.utils.handlerOrRegistered

/** Analog comparator signal (0..15) of the container at [position], or 0 if it is not a container. */
internal fun containerSignal(
    instance: Instance,
    position: Point,
): Int {
    val block = instance.getBlock(position)
    if (block.air()) return 0

    val holder = block.handlerOrRegistered() as? BlockInventoryHolder ?: return 0
    return holder.getInventory(instance, position).comparatorSignal()
}
