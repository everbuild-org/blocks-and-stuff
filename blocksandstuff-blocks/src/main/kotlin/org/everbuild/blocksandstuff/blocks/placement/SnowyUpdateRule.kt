package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule

class SnowyUpdateRule(block: Block) : BlockPlacementRule(block) {
    override fun blockUpdate(updateState: UpdateState): Block {
        val snowy = getSnowyState(updateState.instance, updateState.blockPosition)
        return updateState.currentBlock.withProperty("snowy", snowy)
    }

    override fun blockPlace(placementState: PlacementState): Block {
        val snowy = getSnowyState(placementState.instance, placementState.placePosition)
        return placementState.block().withProperty("snowy", snowy)
    }

    private fun getSnowyState(instance: Block.Getter, position: Point): String {
        val abovePosition = position.relative(BlockFace.TOP)
        val aboveBlock = instance.getBlock(abovePosition)
        val isSnow = aboveBlock.compare(Block.SNOW) || aboveBlock.compare(Block.SNOW_BLOCK) || aboveBlock.compare(Block.POWDER_SNOW)
        return isSnow.toString()
    }
}