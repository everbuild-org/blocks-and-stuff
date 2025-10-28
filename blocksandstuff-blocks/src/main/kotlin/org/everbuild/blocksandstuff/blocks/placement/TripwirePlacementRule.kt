package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class TripwirePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val faceList = listOf(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH)
        val collection = mutableListOf<BlockFace>()
        for (i in faceList) {
            val neighborBlock = placementState.instance.getBlock(placementState.placePosition.relative(i))
            if (neighborBlock.compare(Block.TRIPWIRE) || neighborBlock.compare(Block.TRIPWIRE_HOOK)) {
                collection.add(i)
            }
        }
        return placementState.block.withProperties(
            mapOf(
                "east" to if (collection.contains(BlockFace.EAST)) "true" else "false",
                "west" to if (collection.contains(BlockFace.WEST)) "true" else "false",
                "north" to if (collection.contains(BlockFace.NORTH)) "true" else "false",
                "south" to if (collection.contains(BlockFace.SOUTH)) "true" else "false"
            )
        )
    }

    override fun blockUpdate(updateState: UpdateState): Block? {
        val faceList = listOf(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH)
        val collection = mutableListOf<BlockFace>()
        for (i in faceList) {
            val neighborBlock = updateState.instance.getBlock(updateState.blockPosition.relative(i))
            if (neighborBlock.compare(Block.TRIPWIRE) || (neighborBlock.compare(Block.TRIPWIRE_HOOK) && neighborBlock.getProperty("facing")!!.equals(i.oppositeFace.name.lowercase(), true))) {
                collection.add(i)
            }
        }
        return updateState.currentBlock.withProperties(
            mapOf(
                "east" to if (collection.contains(BlockFace.EAST)) "true" else "false",
                "west" to if (collection.contains(BlockFace.WEST)) "true" else "false",
                "north" to if (collection.contains(BlockFace.NORTH)) "true" else "false",
                "south" to if (collection.contains(BlockFace.SOUTH)) "true" else "false"
            )
        )
    }
}