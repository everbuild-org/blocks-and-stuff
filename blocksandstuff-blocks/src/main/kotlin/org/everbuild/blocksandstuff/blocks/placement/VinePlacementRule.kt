package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.blocks.placement.common.AbstractPlaceAroundCursorRule

class VinePlacementRule(block: Block) : AbstractPlaceAroundCursorRule(
    block, setOf(
        Direction.UP,
        Direction.EAST,
        Direction.WEST,
        Direction.NORTH,
        Direction.SOUTH
    )
) {
    override fun isSupported(
        instance: Block.Getter,
        position: Point,
        direction: Direction
    ): Boolean = isBackSupported(instance, position, direction) || isTopSupported(instance, position, direction)

    override fun isPlacementSupported(
        instance: Block.Getter,
        position: Point,
        direction: Direction
    ): Boolean = isBackSupported(instance, position, direction)

    private fun isTopSupported(instance: Block.Getter, position: Point, direction: Direction): Boolean {
        val block = instance.getBlock(position.add(0.0, 1.0, 0.0))
        if (!block.compare(Block.VINE)) return false
        return block.getProperty(direction.name.lowercase()) == "true"
    }
}