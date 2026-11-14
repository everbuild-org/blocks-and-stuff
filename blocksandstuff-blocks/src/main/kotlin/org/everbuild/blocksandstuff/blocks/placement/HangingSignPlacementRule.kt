package org.everbuild.blocksandstuff.blocks.placement

import kotlin.math.abs
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.getYaw
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.blocksandstuff.common.utils.rotateL
import org.everbuild.blocksandstuff.common.utils.rotateR
import org.everbuild.blocksandstuff.common.utils.sixteenStepRotation

class HangingSignPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(state: PlacementState): Block? {
        val face = state.blockFace ?: return null
        val waterlogged = state.instance.getBlock(state.placePosition).isWater().toString()

        if (face == BlockFace.BOTTOM) return placeCeiling(state, waterlogged)
        if (!face.toDirection().horizontal()) return null

        return placeWall(state, face, waterlogged) ?: placeCeiling(state, waterlogged)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val block = updateState.currentBlock
        val supported =
            if (block.getProperty("facing") != null) {
                canWallHangingSignSurvive(
                    updateState.instance,
                    updateState.blockPosition,
                    BlockFace.valueOf(block.getProperty("facing")!!.uppercase()).toDirection(),
                )
            } else {
                !updateState.instance.getBlock(updateState.blockPosition.add(0.0, 1.0, 0.0)).air()
            }

        if (!supported) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return block
    }

    private fun placeCeiling(state: PlacementState, waterlogged: String): Block? {
        val above = state.instance.getBlock(state.placePosition.add(0.0, 1.0, 0.0))
        if (above.air()) return null

        val isAboveFull = above.collisionShape().isFaceFull(BlockFace.BOTTOM)
        val attached = !isAboveFull
        val rotation =
            if (attached) {
                (state.sixteenStepRotation() + 8) % 16
            } else {
                val direction = BlockFace.fromYaw(state.playerPosition?.yaw ?: 0f).toDirection()
                rotationSegment(direction.opposite().getYaw())
            }

        return state.block
            .withProperty("rotation", rotation.toString())
            .withProperty("attached", attached.toString())
            .withProperty("waterlogged", waterlogged)
    }

    private fun placeWall(state: PlacementState, clickedFace: BlockFace, waterlogged: String): Block? {
        val playerYaw = state.playerPosition?.yaw ?: 0f
        for (direction in nearestLookingDirections(playerYaw, clickedFace)) {
            if (sameAxis(direction, clickedFace.toDirection())) continue
            val facing = direction.opposite()
            if (!canWallHangingSignSurvive(state.instance, state.placePosition, facing)) continue
            val wall = WALL_HANGING_SIGNS[state.block.key()] ?: state.block
            return wall
                .withHandler(state.block.handler())
                .withNbt(state.block.nbt())
                .withProperty("facing", facing.name.lowercase())
                .withProperty("waterlogged", waterlogged)
        }
        return null
    }

    private fun canWallHangingSignSurvive(instance: Block.Getter, pos: Point, facing: Direction): Boolean {
        val clockwise = facing.rotateR()
        val counter = facing.rotateL()
        return canAttachTo(instance, pos.add(clockwise.vec()), counter, facing) ||
            canAttachTo(instance, pos.add(counter.vec()), clockwise, facing)
    }

    private fun canAttachTo(
        instance: Block.Getter,
        attachPos: Point,
        attachFace: Direction,
        signFacing: Direction,
    ): Boolean {
        val neighbor = instance.getBlock(attachPos)
        if (neighbor.name().contains("wall_hanging_sign")) {
            val neighborFacing =
                neighbor.getProperty("facing")
                    ?.let { BlockFace.valueOf(it.uppercase()).toDirection() }
                    ?: return false
            return sameAxis(neighborFacing, signFacing)
        }
        return neighbor.collisionShape().isFaceFull(BlockFace.fromDirection(attachFace))
    }

    private fun nearestLookingDirections(yaw: Float, clickedFace: BlockFace): List<Direction> {
        val cardinals = listOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
        val directions = cardinals.sortedBy { abs(angleDifference(yaw, it.getYaw())) }.toMutableList()
        val clickedDirection = clickedFace.toDirection()
        if (clickedDirection.horizontal()) {
            val opposite = clickedDirection.opposite()
            directions.remove(opposite)
            directions.add(0, opposite)
        }
        return directions
    }

    private fun angleDifference(a: Float, b: Float): Float {
        val difference = (a - b) % 360f
        return when {
            difference < -180f -> difference + 360f
            difference > 180f -> difference - 360f
            else -> difference
        }
    }

    private fun rotationSegment(yaw: Float): Int {
        val normalized = ((yaw % 360f) + 360f) % 360f
        return (normalized / 22.5f + 0.5f).toInt() and 15
    }

    private fun sameAxis(a: Direction, b: Direction): Boolean =
        (a.vec().x() != 0.0) == (b.vec().x() != 0.0)

    companion object {
        val WALL_HANGING_SIGNS =
            mapOf(
                Block.ACACIA_HANGING_SIGN.key() to Block.ACACIA_WALL_HANGING_SIGN,
                Block.BAMBOO_HANGING_SIGN.key() to Block.BAMBOO_WALL_HANGING_SIGN,
                Block.BIRCH_HANGING_SIGN.key() to Block.BIRCH_WALL_HANGING_SIGN,
                Block.CHERRY_HANGING_SIGN.key() to Block.CHERRY_WALL_HANGING_SIGN,
                Block.CRIMSON_HANGING_SIGN.key() to Block.CRIMSON_WALL_HANGING_SIGN,
                Block.DARK_OAK_HANGING_SIGN.key() to Block.DARK_OAK_WALL_HANGING_SIGN,
                Block.JUNGLE_HANGING_SIGN.key() to Block.JUNGLE_WALL_HANGING_SIGN,
                Block.MANGROVE_HANGING_SIGN.key() to Block.MANGROVE_WALL_HANGING_SIGN,
                Block.OAK_HANGING_SIGN.key() to Block.OAK_WALL_HANGING_SIGN,
                Block.PALE_OAK_HANGING_SIGN.key() to Block.PALE_OAK_WALL_HANGING_SIGN,
                Block.POPLAR_HANGING_SIGN.key() to Block.POPLAR_WALL_HANGING_SIGN,
                Block.SPRUCE_HANGING_SIGN.key() to Block.SPRUCE_WALL_HANGING_SIGN,
                Block.WARPED_HANGING_SIGN.key() to Block.WARPED_WALL_HANGING_SIGN,
            )
    }
}
