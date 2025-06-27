package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.tag.BlockTags

class BambooPlantPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val plantableOn = BlockTags.getTaggedWith("minecraft:bamboo_plantable_on")
    private val bamboo = BlockTags.getTaggedWith("blocksandstuff:bamboo")

    override fun blockPlace(placementState: PlacementState): Block? {
        val positionBelow = placementState.placePosition.sub(0.0, 1.0, 0.0)
        val blockBelow = placementState.instance.getBlock(positionBelow)
        val instance = placementState.instance as Instance
        if (bamboo.any { blockBelow.compare(it) }) {
            if (blockBelow.compare(Block.BAMBOO_SAPLING)) {
                instance.setBlock(positionBelow, Block.BAMBOO)
            }

            return placementState.block
        }

        if (plantableOn.any { blockBelow.compare(it) }) {
            return Block.BAMBOO_SAPLING
        }

        return null
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val below = updateState.instance.getBlock(updateState.blockPosition.sub(0.0, 1.0, 0.0))
        if (plantableOn.none { it.compare(below) }) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return updateState.currentBlock
    }

    override fun maxUpdateDistance(): Int = 500
}