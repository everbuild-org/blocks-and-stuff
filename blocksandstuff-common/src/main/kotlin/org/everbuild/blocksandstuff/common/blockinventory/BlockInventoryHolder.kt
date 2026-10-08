package org.everbuild.blocksandstuff.common.blockinventory

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance

interface BlockInventoryHolder {
    fun getInventory(instance: Instance, blockPos: Point): PhysicalInventory
}