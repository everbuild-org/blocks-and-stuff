package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.TagKey

class FarmlandPlacementRule(block: Block) : BlockPlacementRule(block) {
    val maintainsFarmLand = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:maintains_farmland"))!!
    override fun blockUpdate(updateState: UpdateState): Block {
        return checkBlockPlacement(updateState.blockPosition, updateState.instance) ?: updateState.currentBlock
    }

    override fun blockPlace(placementState: PlacementState): Block {
        return checkBlockPlacement(placementState.placePosition, placementState.instance) ?: placementState.block()
    }

    fun checkBlockPlacement(placePosition: Point, instance: Block.Getter): Block? {
        val abovePosition = placePosition.relative(BlockFace.TOP)
        val aboveBlock = instance.getBlock(abovePosition)
        if (!aboveBlock.isAir && !maintainsFarmLand.contains(aboveBlock)) {
            return Block.DIRT
        }
        return null
    }
}