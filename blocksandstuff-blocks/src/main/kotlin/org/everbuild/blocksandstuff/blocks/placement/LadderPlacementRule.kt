package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class LadderPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockFace = placementState.blockFace() ?: return null

        val supporting = placementState.placePosition.add(blockFace.oppositeFace.toDirection().vec())
        if (!placementState.instance.getBlock(supporting).registry()!!.collisionShape().isFaceFull(blockFace)) {
            return null
        }
        return block
            .withProperty("facing", blockFace.name.lowercase())
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val facing = BlockFace.valueOf(updateState.currentBlock.getProperty("facing")!!.uppercase())
        val supportingBlockPos = updateState.blockPosition.add(facing.oppositeFace.toDirection().vec())

        if (!updateState.instance.getBlock(supportingBlockPos).isSolid) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }
}