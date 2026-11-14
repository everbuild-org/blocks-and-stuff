package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

internal fun BlockPlacementRule.PlacementState.upperHalfPositionIfSupported(): Point? {
    val upperPosition = placePosition.add(0.0, 1.0, 0.0)
    if (!instance.getBlock(upperPosition).replaceable()) return null

    val support = instance.getBlock(placePosition.sub(0.0, 1.0, 0.0))
    if (!support.collisionShape().isFaceFull(BlockFace.TOP)) return null

    return upperPosition
}

internal fun BlockPlacementRule.UpdateState.removeUnsupportedLowerHalf(): Boolean {
    if (currentBlock.getProperty("half") != "lower") return false
    val support = instance.getBlock(blockPosition.relative(BlockFace.BOTTOM))
    if (support.collisionShape().isFaceFull(BlockFace.TOP)) return false

    DroppedItemFactory.maybeDrop(this)
    val world = instance as Instance
    world.setBlock(blockPosition.relative(BlockFace.TOP), Block.AIR)
    world.setBlock(blockPosition, Block.AIR)
    return true
}
