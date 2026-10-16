package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance

internal object GrowthLight {
    fun hasEnoughLight(
        instance: Instance,
        position: Point,
        minLight: Int,
    ): Boolean {
        if (minLight <= 0) return true
        val x = position.blockX()
        val y = position.blockY()
        val z = position.blockZ()
        return maxOf(instance.getSkyLight(x, y, z), instance.getBlockLight(x, y, z)) >= minLight
    }
}
