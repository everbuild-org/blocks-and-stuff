package org.everbuild.blocksandstuff.common.utils

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity

fun Entity.eyePosition(): Pos {
    if (this.isSneaking) return position.add(0.0, 1.23, 0.0)
    return position.add(0.0, 1.53, 0.0)
}