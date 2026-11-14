package org.everbuild.blocksandstuff.blocks.util

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity

internal fun Entity.intersectsBlock(position: Point): Boolean {
    val box = boundingBox
    val origin = this.position
    val minX = position.x()
    val minY = position.y()
    val minZ = position.z()
    return origin.x() + box.maxX() > minX && origin.x() + box.minX() < minX + 1.0 &&
        origin.y() + box.maxY() > minY && origin.y() + box.minY() < minY + 1.0 &&
        origin.z() + box.maxZ() > minZ && origin.z() + box.minZ() < minZ + 1.0
}
