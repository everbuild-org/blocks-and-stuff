package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.blocks.placement.util.States
import org.everbuild.blocksandstuff.common.utils.isWater
import java.util.*
import java.util.Map

class StairsPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockUpdate(updateState: UpdateState): Block {
        return updateState.currentBlock().withProperty(
            States.SHAPE,
            getShape(updateState.instance(), updateState.currentBlock(), updateState.blockPosition())
        )
    }

    override fun blockPlace(placementState: PlacementState): Block? {
        val placementFace = placementState.blockFace()
        val placementPos = placementState.placePosition()
        val cursorPos = placementState.cursorPosition() ?: Vec.ZERO
        val playerPos = placementState.playerPosition() ?: Pos.ZERO

        val half = if (placementFace == BlockFace.BOTTOM
            ||
            placementFace != BlockFace.TOP
            &&
            cursorPos.y() > 0.5
        )
            BlockFace.TOP
        else
            BlockFace.BOTTOM
        val facing = BlockFace.fromYaw(playerPos.yaw())

        var block: Block = this.block.withProperties(
            Map.of(
                States.HALF, half.name.lowercase(Locale.getDefault()),
                States.FACING, facing.name.lowercase(Locale.getDefault())
            )
        )

        block = block.withProperty(States.SHAPE, getShape(placementState.instance(), block, placementPos))
        val isInWater = placementState.instance().getBlock(placementPos).isWater()
        return block.withProperty("waterlogged", isInWater.toString())
    }

    private fun getShape(instance: Block.Getter, block: Block, blockPos: Point): String {
        val direction: Direction = States.getFacing(block).toDirection()
        val offsetBlock = instance.getBlock(
            blockPos.add(
                direction.normalX().toDouble(),
                direction.normalY().toDouble(),
                direction.normalZ().toDouble()
            )
        )
        val offsetDirection: Direction = States.getFacing(offsetBlock).toDirection()
        val oppositeOffsetBlock = instance.getBlock(
            blockPos.add(
                direction.opposite().normalX().toDouble(),
                direction.opposite().normalY().toDouble(),
                direction.opposite().normalZ().toDouble()
            )
        )
        val oppositeOffsetDirection: Direction = States.getFacing(oppositeOffsetBlock).toDirection()

        if (isStairs(offsetBlock)
            && States.getHalf(block) === States.getHalf(offsetBlock) && States.getAxis(offsetDirection) !== States.getAxis(
                direction
            ) &&
            isDifferentOrientation(instance, block, blockPos, offsetDirection.opposite())
        ) {
            return if (offsetDirection == States.rotateYCounterclockwise(direction)) {
                "outer_left"
            } else {
                "outer_right"
            }
        }

        if (isStairs(oppositeOffsetBlock)
            && States.getHalf(block) === States.getHalf(oppositeOffsetBlock) && States.getAxis(oppositeOffsetDirection) !== States.getAxis(
                direction
            ) &&
            isDifferentOrientation(instance, block, blockPos, oppositeOffsetDirection)
        ) {
            return if (oppositeOffsetDirection == States.rotateYCounterclockwise(direction)) {
                "inner_left"
            } else {
                "inner_right"
            }
        }

        return "straight"
    }

    private fun isDifferentOrientation(
        instance: Block.Getter,
        block: Block,
        blockPos: Point,
        direction: Direction
    ): Boolean {
        val facing: BlockFace? = States.getFacing(block)
        val half: BlockFace? = States.getHalf(block)
        val instanceBlock = instance.getBlock(
            blockPos.add(
                direction.normalX().toDouble(),
                direction.normalY().toDouble(),
                direction.normalZ().toDouble()
            )
        )
        val instanceBlockFacing: BlockFace? = States.getFacing(instanceBlock)
        val instanceBlockHalf: BlockFace? = States.getHalf(instanceBlock)

        return !isStairs(instanceBlock) || instanceBlockFacing != facing || instanceBlockHalf != half
    }

    private fun isStairs(block: Block): Boolean {
        return block.name().endsWith("_stairs")
    }
}