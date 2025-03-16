package org.everbuild.blocksandstuff.common.item

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

interface DroppedItemFactory {
    fun spawn(instance: Instance, position: Point, block: Block)

    companion object {
        var current: DroppedItemFactory = DefaultDroppedItemFactory()
    }
}