package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.blocks.placement.common.AbstractPlaceAroundCursorRule

class GlowLichenPlacementRule(block: Block) : AbstractPlaceAroundCursorRule(block, Direction.entries.toSet()) {
    override fun isSupported(
        instance: Block.Getter,
        position: Point,
        direction: Direction
    ): Boolean = isBackSupported(instance, position, direction)

    override fun isPlacementSupported(
        instance: Block.Getter,
        position: Point,
        direction: Direction
    ): Boolean = isBackSupported(instance, position, direction)
}