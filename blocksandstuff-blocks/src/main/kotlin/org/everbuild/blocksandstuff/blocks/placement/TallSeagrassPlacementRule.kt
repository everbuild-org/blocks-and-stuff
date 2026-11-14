package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class TallSeagrassPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        val instance = placementState.instance as Instance
        val position = placementState.placePosition
        val above = position.relative(BlockFace.TOP)

        if (!instance.getBlock(position).compare(Block.WATER) || !instance.getBlock(above).compare(Block.WATER)) {
            return instance.getBlock(position)
        }
        if (!supportsSeagrass(instance.getBlock(position.relative(BlockFace.BOTTOM)))) {
            return instance.getBlock(position)
        }

        instance.setBlock(above, placementState.block.withProperty("half", "upper"), false)
        return placementState.block.withProperty("half", "lower")
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val instance = updateState.instance as Instance
        val current = updateState.currentBlock
        if (!current.compare(Block.TALL_SEAGRASS)) return current

        val half = current.getProperty("half")
        val survives =
            if (half == "lower") {
                val upper = instance.getBlock(updateState.blockPosition.relative(BlockFace.TOP))
                supportsSeagrass(instance.getBlock(updateState.blockPosition.relative(BlockFace.BOTTOM))) &&
                    upper.compare(Block.TALL_SEAGRASS) &&
                    upper.getProperty("half") == "upper"
            } else {
                val lower = instance.getBlock(updateState.blockPosition.relative(BlockFace.BOTTOM))
                lower.compare(Block.TALL_SEAGRASS) && lower.getProperty("half") == "lower"
            }

        if (survives) return current

        val otherHalfPosition: Point =
            if (half == "lower") {
                updateState.blockPosition.relative(BlockFace.TOP)
            } else {
                updateState.blockPosition.relative(BlockFace.BOTTOM)
            }
        val otherHalf = instance.getBlock(otherHalfPosition)
        if (otherHalf.compare(Block.TALL_SEAGRASS)) {
            instance.setBlock(otherHalfPosition, Block.WATER, false)
        }

        DroppedItemFactory.maybeDrop(instance, updateState.blockPosition, ItemStack.of(Material.SEAGRASS, 2))
        return Block.WATER
    }

    private fun supportsSeagrass(block: Block): Boolean =
        block.collisionShape().isFaceFull(BlockFace.TOP) && !block.compare(Block.MAGMA_BLOCK)
}
