package org.everbuild.blocksandstuff.blocks.util

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHolder
import org.everbuild.blocksandstuff.common.blockinventory.comparatorSignal

/** Analog comparator signal (0..15) of the container at [position], or 0 if it is not a container. */
internal fun containerSignal(
    instance: Instance,
    position: Point,
): Int {
    val block = instance.getBlock(position)
    if (block.air()) return 0

    val handler =
        block.handler()
            ?: MinecraftServer.getBlockManager().getHandler(block.key().asString())
    val holder = handler as? BlockInventoryHolder ?: return 0
    return holder.getInventory(instance, position).comparatorSignal()
}
