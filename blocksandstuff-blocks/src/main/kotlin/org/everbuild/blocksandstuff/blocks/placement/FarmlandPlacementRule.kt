package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.TagKey
import org.everbuild.blocksandstuff.common.utils.isWater

class FarmlandPlacementRule(
    block: Block,
) : BlockPlacementRule(block) {
    val maintainsFarmLand = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:maintains_farmland"))!!

    override fun blockUpdate(updateState: UpdateState): Block =
        checkBlockPlacement(updateState.blockPosition, updateState.instance) ?: updateState.currentBlock

    override fun blockPlace(placementState: PlacementState): Block =
        checkBlockPlacement(placementState.placePosition, placementState.instance) ?: placementState.block

    fun checkBlockPlacement(
        placePosition: Point,
        instance: Block.Getter,
    ): Block? {
        val abovePosition = placePosition.add(0.0, 1.0, 0.0)
        val aboveBlock = instance.getBlock(abovePosition)
        if (!aboveBlock.isAir && !aboveBlock.isWater() && aboveBlock.isSolid) {
            return Block.DIRT
        }
        return null
    }
}
