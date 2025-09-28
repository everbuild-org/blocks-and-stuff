package org.everbuild.blocksandstuff.common.utils

import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.instance.block.rule.BlockPlacementRule.PlacementState
import net.minestom.server.utils.Direction

fun Direction.getAxis(): String {
    return when (this) {
        Direction.UP, Direction.DOWN -> "y"
        Direction.NORTH, Direction.SOUTH -> "z"
        Direction.EAST, Direction.WEST -> "x"
    }
}

fun PlacementState.getHorizontalPlacementDirection(): Direction? {
    val yaw = this.playerPosition?.yaw ?: return null
    return BlockFace.fromYaw(yaw).toDirection()
}

fun PlacementState.sixteenStepRotation(): Int {
    var yaw = this.playerPosition?.yaw ?: return 0
    yaw += 22.5f / 2.0f
    if (yaw < 0) yaw += 360f
    return (yaw / 22.5f).toInt().coerceIn(0, 15)
}

fun PlacementState.canAttach(): Boolean {
    val anchor = this.placePosition.sub((this.blockFace ?: return false).toDirection().vec())
    val anchorBlock = this.instance.getBlock(anchor)
    return anchorBlock.registry().collisionShape().isFaceFull(this.blockFace!!)
}

fun BlockPlacementRule.UpdateState.canAttach(facing: BlockFace): Boolean {
    val anchor = this.blockPosition.sub(facing.toDirection().vec())
    val anchorBlock = this.instance.getBlock(anchor)
    return anchorBlock.registry().collisionShape().isFaceFull(facing)
}

private fun getNearestLookingDirection(position: Pos, allowedDirections: Collection<Direction>): Direction {
    return allowedDirections.minBy { it.vec().scalarProduct(position.direction().normalize()) }
}

fun PlacementState.getNearestLookingDirection(allowedDirections: Collection<Direction>): Direction {
    return getNearestLookingDirection(this.playerPosition!!, allowedDirections)
}

fun PlacementState.getNearestLookingDirection(): Direction {
    this.playerPosition ?: return Direction.EAST
    return this.getNearestLookingDirection(Direction.entries)
}

fun PlacementState.getNearestHorizontalLookingDirection(): Direction {
    this.playerPosition ?: return Direction.EAST
    return this.getNearestLookingDirection(Direction.HORIZONTAL.iterator().asSequence().toList())
}

fun BlockHandler.Interaction.getNearestLookingDirection(allowedDirections: Collection<Direction>): Direction {
    return getNearestLookingDirection(this.player.position, allowedDirections)
}

fun BlockHandler.Interaction.getNearestHorizontalLookingDirection(): Direction {
    return getNearestLookingDirection(this.player.position, Direction.HORIZONTAL.iterator().asSequence().toList())
}

fun Vec.scalarProduct(rhs: Vec): Double {
    val componentsMultiplied = this.mul(rhs)
    return componentsMultiplied.x + componentsMultiplied.y + componentsMultiplied.z
}

fun Direction.rotateR(): Direction {
    return when (this) {
        Direction.NORTH -> Direction.EAST
        Direction.EAST -> Direction.SOUTH
        Direction.SOUTH -> Direction.WEST
        Direction.WEST -> Direction.NORTH
        else -> this
    }
}

fun Direction.rotateL(): Direction {
    return this.opposite().rotateR()
}

fun Direction.getYaw(): Float {
    return when (this) {
        Direction.UP -> 0f
        Direction.DOWN -> 0f
        Direction.NORTH -> 180f
        Direction.EAST -> -90f
        Direction.SOUTH -> 0f
        Direction.WEST -> 90f
    }
}
