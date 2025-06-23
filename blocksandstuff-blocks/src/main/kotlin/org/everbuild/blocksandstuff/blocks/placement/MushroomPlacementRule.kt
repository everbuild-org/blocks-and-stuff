package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class MushroomPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val faceMap = mapOf(
        "north" to BlockFace.NORTH,
        "south" to BlockFace.SOUTH,
        "east" to BlockFace.EAST,
        "west" to BlockFace.WEST,
        "up" to BlockFace.TOP,
        "down" to BlockFace.BOTTOM
    )

    override fun blockPlace(placementState: PlacementState): Block {
        return getState(placementState.instance, placementState.placePosition, placementState.block, true)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        return getState(updateState.instance, updateState.blockPosition, updateState.currentBlock, false)
    }

    private fun getState(instance: Block.Getter, position: Point, currentBlock: Block, isPlacement: Boolean): Block {
        var newBlock = currentBlock
        for ((facePropertyName, face) in faceMap) {
            if (isPlacement || newBlock.getProperty(facePropertyName) == "true") {
                val neighborPos = position.relative(face)
                val neighborBlock = instance.getBlock(neighborPos)
                val shouldConnect = canConnect(currentBlock, neighborBlock)
                val propertyValue = if (shouldConnect) "false" else "true"
                newBlock = newBlock.withProperty(facePropertyName, propertyValue)
            }
        }
        return newBlock
    }

    private fun canConnect(currentBlock: Block, neighborBlock: Block): Boolean {
        if (neighborBlock.compare(Block.MUSHROOM_STEM)) {
            return true
        }

        val isCurrentBrown = currentBlock.compare(Block.BROWN_MUSHROOM_BLOCK)
        val isNeighborBrown = neighborBlock.compare(Block.BROWN_MUSHROOM_BLOCK)
        if (isCurrentBrown) {
            return isNeighborBrown
        }

        val isCurrentRed = currentBlock.compare(Block.RED_MUSHROOM_BLOCK)
        val isNeighborRed = neighborBlock.compare(Block.RED_MUSHROOM_BLOCK)
        if (isCurrentRed) {
            return isNeighborRed
        }
        return false
    }
}