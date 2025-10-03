package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.blocksandstuff.common.utils.rotateL
import org.everbuild.blocksandstuff.common.utils.rotateR

class ChestPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val clicked = placementState.instance.getBlock(
            placementState.placePosition.relative(placementState.blockFace!!.oppositeFace)
        )

        val clickedFacing = getClickedChestFacing(placementState, clicked)
        val facing = clickedFacing ?: placementState.getNearestHorizontalLookingDirection()
        val waterlogged = placementState.instance.getBlock(placementState.placePosition).isWater()
        val facedBlock = placementState.block
            .withProperty("facing", facing.toString().lowercase())
            .withProperty("waterlogged", waterlogged.toString())

        if (placementState.isPlayerShifting && clickedFacing == null) return facedBlock

        if (clickedFacing != null && canConnect(placementState, facing, placementState.blockFace!!.oppositeFace.toDirection())) {
            return connect(
                placementState,
                facing,
                placementState.blockFace!!.oppositeFace.toDirection(),
                facedBlock
            )
        }

        if (canConnect(placementState, facing, facing.rotateR())) return connect(
            placementState,
            facing,
            facing.rotateR(),
            facedBlock
        )

        if (canConnect(placementState, facing, facing.rotateL())) return connect(
            placementState,
            facing,
            facing.rotateL(),
            facedBlock
        )

        return facedBlock
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val type = updateState.currentBlock.getProperty("type")
        if (type == "single") return super.blockUpdate(updateState)
        val facing = Direction.valueOf(updateState.currentBlock.getProperty("facing")!!.uppercase())

        val (neighbourPosition, expectedNeighbourType) = if (type == "left") {
            facing.rotateR() to "right"
        } else {
            facing.rotateL() to "left"
        }

        val neighbourBlockPos = updateState.blockPosition.relative(BlockFace.fromDirection(neighbourPosition))
        val neighbourBlock = updateState.instance.getBlock(neighbourBlockPos)

        if ((!neighbourBlock.compare(block, Block.Comparator.ID))
            || neighbourBlock.getProperty("facing") != facing.toString().lowercase()
            || neighbourBlock.getProperty("type") != expectedNeighbourType
        ) {
            return updateState.currentBlock
                .withProperty("type", "single")
        }

        return super.blockUpdate(updateState)
    }

    private fun getClickedChestFacing(placementState: PlacementState, clicked: Block): Direction? {
        if (!clicked.compare(block, Block.Comparator.ID)) return null
        val facing = Direction.valueOf(clicked.getProperty("facing")!!.uppercase())
        val lrFaces = listOf(facing.rotateL(), facing.rotateR()).map { BlockFace.fromDirection(it) }
        return if (lrFaces.contains(placementState.blockFace)) facing else null
    }

    private fun canConnect(placementState: PlacementState, facingSelf: Direction, connecting: Direction): Boolean {
        val blockPos = placementState.placePosition.relative(BlockFace.fromDirection(connecting))
        val currentBlock = placementState.instance.getBlock(blockPos)
        if (!currentBlock.compare(block, Block.Comparator.ID)) return false
        val facing = Direction.valueOf(currentBlock.getProperty("facing")!!.uppercase())
        if (facing != facingSelf) return false
        val type = currentBlock.getProperty("type")
        return type == "single"
    }

    private fun connect(placementState: PlacementState, facingSelf: Direction, connecting: Direction, facedBlock: Block): Block {
        val blockPos = placementState.placePosition.relative(BlockFace.fromDirection(connecting))
        val (selfType, otherType) = if (connecting == facingSelf.rotateL()) {
            "right" to "left"
        } else {
            "left" to "right"
        }

        val connectingBlock = placementState.instance.getBlock(blockPos)

        (placementState.instance as Instance).setBlock(
            blockPos, connectingBlock
                .withProperty("type", otherType)
        )

        return facedBlock
            .withProperty("type", selfType)
    }

}