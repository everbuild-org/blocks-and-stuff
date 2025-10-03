package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.TagKey
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection
import org.everbuild.blocksandstuff.common.utils.rotateL
import org.everbuild.blocksandstuff.common.utils.rotateR

class FenceGatePlacementRule(block: Block) : BlockPlacementRule(block) {
    private val walls = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:walls"))!!

    override fun blockPlace(state: PlacementState): Block? {
        val direction = state.getNearestHorizontalLookingDirection().opposite()
        val block = state.block
            .withProperty("facing", direction.toString().lowercase())

        return integrateInWalls(state.instance, state.placePosition, block)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        return integrateInWalls(updateState.instance, updateState.blockPosition, updateState.currentBlock)
    }

    private fun integrateInWalls(instance: Block.Getter, pos: Point, block: Block): Block {
        val direction = Direction.valueOf(block.getProperty("facing")!!.uppercase())
        val leftBlock = instance.getBlock(pos.add(direction.rotateR().vec()))
        val rightBlock = instance.getBlock(pos.add(direction.rotateL().vec()))
        val inWall = walls.contains(leftBlock) || walls.contains(rightBlock)

        return block.withProperty("in_wall", inWall.toString())
    }
}