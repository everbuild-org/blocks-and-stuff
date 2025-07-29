package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.RegistryTag
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler

class TopAttachedVinePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (!validatePosition(placementState.instance, placementState.placePosition)) {
            return null
        }

        return placementState.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (!validatePosition(updateState.instance, updateState.blockPosition)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        val below = updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))

        if (PLANT_BLOCKS.contains(updateState.currentBlock) && (!below.compare(VINE_BLOCK_MAP[updateState.currentBlock.defaultState()]!!, Block.Comparator.ID) && !below.compare(updateState.currentBlock, Block.Comparator.ID))) {
            return VINE_BLOCK_MAP[updateState.currentBlock.defaultState()]!!.withDefaultHandler()
        } else if (VINE_BLOCKS.contains(updateState.currentBlock) && below.compare(updateState.currentBlock, Block.Comparator.ID)) {
            return PLANT_BLOCK_MAP[updateState.currentBlock.defaultState()]!!.withDefaultHandler()
        }

        return updateState.currentBlock
    }

    fun validatePosition(instance: Block.Getter, position: Point): Boolean {
        val above = instance.getBlock(position.add(0.0, 1.0, 0.0))

        if (above.registry().collisionShape().isFaceFull(BlockFace.BOTTOM)) return true
        if (above.compare(block, Block.Comparator.ID) || above.compare(OTHER_MAP[block]!!, Block.Comparator.ID)) return true
        if (above.key().value().substring(0, 3) == block.key().value().substring(0, 3)) return true
        return false
    }

    companion object {
        private val PLANT_BLOCK_MAP = mapOf(
            Block.CAVE_VINES to Block.CAVE_VINES_PLANT,
            Block.WEEPING_VINES to Block.WEEPING_VINES_PLANT,
        )

        private val VINE_BLOCK_MAP = PLANT_BLOCK_MAP.map { it.value to it.key }.toMap()
        private val OTHER_MAP = (PLANT_BLOCK_MAP.map { it.key to it.value } + VINE_BLOCK_MAP.map { it.key to it.value }).toMap()

        private val PLANT_BLOCKS = RegistryTag.direct(*PLANT_BLOCK_MAP.values.toTypedArray())
        private val VINE_BLOCKS = RegistryTag.direct(*VINE_BLOCK_MAP.values.toTypedArray())
    }
}