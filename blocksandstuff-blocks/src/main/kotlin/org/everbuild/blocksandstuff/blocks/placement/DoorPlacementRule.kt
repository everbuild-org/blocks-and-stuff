package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import org.everbuild.blocksandstuff.common.utils.rotateL
import org.everbuild.blocksandstuff.common.utils.rotateR

class DoorPlacementRule(baseDoorBlock: Block) : BlockPlacementRule(baseDoorBlock) {
    private fun countSolidFaces(instance: Instance, centerPos: Point, horizontalDirection: BlockFace): Int {
        var solidFaces = 0

        if (instance.getBlock(centerPos.relative(horizontalDirection)).isSolid) {
            solidFaces++
        }

        val directionVector = horizontalDirection.toDirection()

        val diagClockwiseDir = BlockFace.fromDirection(directionVector.rotateR())
        if (instance.getBlock(centerPos.relative(diagClockwiseDir)).isSolid) {
            solidFaces++
        }

        val diagCounterClockwiseDir = BlockFace.fromDirection(directionVector.rotateL())
        if (instance.getBlock(centerPos.relative(diagCounterClockwiseDir)).isSolid) {
            solidFaces++
        }

        return solidFaces
    }

    private fun getHingeSide(instance: Instance, placePos: Point, cursorPos: Point?, playerFacing: BlockFace): String {
        val doorFrontDirection = playerFacing.oppositeFace

        val leftOfDoor = BlockFace.fromDirection(doorFrontDirection.toDirection().rotateL())
        val rightOfDoor = BlockFace.fromDirection(doorFrontDirection.toDirection().rotateR())

        val leftBlockPos = placePos.relative(leftOfDoor)
        val rightBlockPos = placePos.relative(rightOfDoor)

        val leftNeighborBlock = instance.getBlock(leftBlockPos)
        val rightNeighborBlock = instance.getBlock(rightBlockPos)

        if (leftNeighborBlock.isAir || rightNeighborBlock.isAir) {
            if (leftNeighborBlock.key() == block.key()) {
                val existingDoorHalf = leftNeighborBlock.getProperty("half")

                if (existingDoorHalf == "lower") {
                    return "left"
                }
            }

            if (rightNeighborBlock.key() == block.key()) {
                val existingDoorHalf = rightNeighborBlock.getProperty("half")

                if (existingDoorHalf == "lower") {
                    return "right"
                }
            }
        }

        val playerRightSideBlockFace = BlockFace.fromDirection(playerFacing.toDirection().rotateR())
        val playerLeftSideBlockFace = BlockFace.fromDirection(playerFacing.toDirection().rotateL())

        val leftSupportScore = countSolidFaces(instance, placePos, playerLeftSideBlockFace)
        val rightSupportScore = countSolidFaces(instance, placePos, playerRightSideBlockFace)

        if (leftSupportScore > rightSupportScore) {
            return "left"
        }
        if (rightSupportScore > leftSupportScore) {
            return "right"
        }

        if (cursorPos == null) {
            return "left"
        }

        val option = when(playerFacing) {
            BlockFace.EAST -> cursorPos.z() < 0.5
            BlockFace.WEST -> cursorPos.z() > 0.5
            BlockFace.NORTH -> cursorPos.x() < 0.5
            BlockFace.SOUTH -> cursorPos.x() > 0.5
            else -> true
        }

        return if (option) {
            "left"
        } else {
            "right"
        }
    }

    override fun blockPlace(placementState: PlacementState): Block? {
        val instance = placementState.instance
        val placePos = placementState.placePosition

        val upperPos = placePos.add(0.0, 1.0, 0.0)
        if (!instance.getBlock(upperPos).registry().isReplaceable) {
            return null
        }

        val lowerPos = placePos.sub(0.0, 1.0, 0.0)
        if (!instance.getBlock(lowerPos).registry().collisionShape().isFaceFull(BlockFace.TOP)) {
            return null
        }

        val facing = placementState.getNearestHorizontalLookingDirection().opposite()

        val hinge = getHingeSide(
            instance as Instance,
            placePos,
            placementState.cursorPosition,
            BlockFace.fromDirection(facing)
        )

        val open = "false"
        val powered = "false"

        val lowerDoorBlock = placementState.block
            .withProperty("facing", facing.name.lowercase())
            .withProperty("half", "lower")
            .withProperty("hinge", hinge)
            .withProperty("open", open)
            .withProperty("powered", powered)

        val upperDoorBlock = placementState.block
            .withProperty("facing", facing.name.lowercase())
            .withProperty("half", "upper")
            .withProperty("hinge", hinge)
            .withProperty("open", open)
            .withProperty("powered", powered)

        instance.setBlock(upperPos, upperDoorBlock)

        return lowerDoorBlock
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val instance = updateState.instance
        val currentBlock = updateState.currentBlock
        val blockPosition = updateState.blockPosition

        val half = currentBlock.getProperty("half")

        val neighborPos: Point
        val expectedOtherHalf: String

        if (half == "lower") {
            neighborPos = blockPosition.add(0.0, 1.0, 0.0)
            expectedOtherHalf = "upper"
        } else { // half == "upper"
            neighborPos = blockPosition.sub(0.0, 1.0, 0.0)
            expectedOtherHalf = "lower"
        }

        val neighborBlock = instance.getBlock(neighborPos)

        if (!neighborBlock.compare(updateState.currentBlock) || neighborBlock.getProperty("half") != expectedOtherHalf) {
            if (neighborBlock.compare(updateState.currentBlock)) {
                (instance as Instance).setBlock(neighborPos, Block.AIR)
            }
            return Block.AIR
        }

        val blockBelow = instance.getBlock(blockPosition.relative(BlockFace.BOTTOM))
        if (updateState.currentBlock.getProperty("half") == "lower" && !blockBelow.registry().collisionShape()
                .isFaceFull(BlockFace.TOP)
        ) {
            DroppedItemFactory.maybeDrop(updateState)
            (instance as Instance).setBlock(neighborPos, Block.AIR)
            instance.setBlock(updateState.blockPosition, Block.AIR)
        }

        return updateState.currentBlock
    }
}