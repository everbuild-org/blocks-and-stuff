package org.everbuild.blocksandstuff.blocks.placement

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class CocoaPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val allowPlace = Block.staticRegistry().getTag(Key.key("minecraft:jungle_logs"))!!
    override fun blockPlace(placementState: PlacementState): Block? {
        val placementFace = placementState.blockFace() ?: return placementState.block()
        val blockPos = placementState.placePosition()
        if (!canSupportCocoa(placementState.instance, blockPos.relative(placementFace.oppositeFace))) {
            return null
        }
        if (!placementFace.toDirection().horizontal()) {
            return null
        }
        return placementState.block().withProperty("facing", placementFace.oppositeFace.toString().lowercase() )
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val blockPos = updateState.blockPosition()
        val supporting = updateState.currentBlock.getProperty("facing")
            ?.let { BlockFace.valueOf(it.uppercase())} ?: return updateState.currentBlock
        if (!canSupportCocoa(updateState.instance, blockPos.relative(supporting))) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return updateState.currentBlock
    }

    private fun canSupportCocoa(instance: Block.Getter, position: Point): Boolean {
        val block = instance.getBlock(position)
        val isJungle = allowPlace.contains(block)
        return isJungle
    }
}