package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class EndRodPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockFace = placementState.blockFace() ?: return null
        val supportingPos = getSupportingBlockPosition(blockFace, placementState.placePosition)
        val supportingBlock = placementState.instance.getBlock(supportingPos)
        if (supportingBlock.compare(Block.END_ROD)) {
            val supportingFacing = supportingBlock.getProperty("facing") ?: "up"
            val supportingFace = runCatching { BlockFace.valueOf(supportingFacing.uppercase()) }.getOrNull()
            val newFacing = when (blockFace) {
                BlockFace.TOP -> if (supportingFacing == "up") "down" else "up"
                BlockFace.BOTTOM -> if (supportingFacing == "down") "up" else "down"
                else -> {
                    if (supportingFace != null && supportingFace == blockFace.oppositeFace) {
                        blockFace.toString().lowercase() // zeigt von der Träger-Spitze weg
                    } else if (supportingFace != null && supportingFace == blockFace) {
                        blockFace.oppositeFace.toString().lowercase()
                    } else {
                        blockFace.toString().lowercase()
                    }
                }
            }
            return block.withProperty("facing", newFacing)
        }
        return block.withProperty(
            "facing",
            when (blockFace) {
                BlockFace.BOTTOM -> "down"
                BlockFace.TOP -> "up"
                else -> blockFace.name.lowercase()
            }
        )
    }

    fun getSupportingBlockPosition(facing: BlockFace, blockPosition: Point): Point {
        return when (facing) {
            BlockFace.BOTTOM -> blockPosition.add(0.0, 1.0, 0.0)
            BlockFace.TOP -> blockPosition.sub(0.0, 1.0, 0.0)
            else -> blockPosition.add(facing.oppositeFace.toDirection().vec())
        }
    }
}