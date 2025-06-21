package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.TagKey
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.blocksandstuff.common.utils.sixteenStepRotation

class SignPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val wallSigns = Block.staticRegistry().getTag(TagKey.ofHash("#minecraft:wall_signs"))!!

    override fun blockPlace(placementState: PlacementState): Block? {
        val currentBlock = placementState.instance.getBlock(placementState.placePosition)
        if (placementState.blockFace == BlockFace.TOP) {
            val direction = ((placementState.sixteenStepRotation() + 8) % 16).toString()
            return supportedOrNull(
                placementState.instance,
                placementState.placePosition,
                placementState.block
                    .withProperty("rotation", direction)
                    .withProperty("waterlogged", currentBlock.isWater().toString())
            )
        } else if (placementState.blockFace!!.toDirection().horizontal()) {
            val facing = placementState.blockFace!!.toString().lowercase()
            val handler = placementState.block.handler()
            val nbt = placementState.block.nbt()
            return supportedOrNull(
                placementState.instance,
                placementState.placePosition,
                WALL_SIGNS[block]!!
                    .withHandler(handler)
                    .withNbt(nbt)
                    .withProperty("facing", facing)
                    .withProperty("waterlogged", currentBlock.isWater().toString())
            )
        } else return null
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (!isSupported(updateState.instance, updateState.currentBlock, updateState.blockPosition)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return updateState.currentBlock
    }

    private fun supportedOrNull(instance: Block.Getter, position: Point, block: Block): Block? {
        return if (isSupported(instance, block, position)) block else null
    }

    private fun isSupported(instance: Block.Getter, block: Block, position: Point): Boolean {
        if (wallSigns.contains(block)) {
            val facing = BlockFace.valueOf(block.getProperty("facing").uppercase())
            val supportingBlockPos = position.add(facing.oppositeFace.toDirection().vec())
            return !instance.getBlock(supportingBlockPos).isAir
        } else {
            val below = instance.getBlock(position.sub(0.0, 1.0, 0.0))
            return below.isSolid
        }
    }

    companion object {
        val WALL_SIGNS = mapOf(
            Block.ACACIA_SIGN to Block.ACACIA_WALL_SIGN,
            Block.BAMBOO_SIGN to Block.BAMBOO_WALL_SIGN,
            Block.BIRCH_SIGN to Block.BIRCH_WALL_SIGN,
            Block.CHERRY_SIGN to Block.CHERRY_WALL_SIGN,
            Block.CRIMSON_SIGN to Block.CRIMSON_WALL_SIGN,
            Block.DARK_OAK_SIGN to Block.DARK_OAK_WALL_SIGN,
            Block.JUNGLE_SIGN to Block.JUNGLE_WALL_SIGN,
            Block.MANGROVE_SIGN to Block.MANGROVE_WALL_SIGN,
            Block.OAK_SIGN to Block.OAK_WALL_SIGN,
            Block.SPRUCE_SIGN to Block.SPRUCE_WALL_SIGN,
            Block.WARPED_SIGN to Block.WARPED_WALL_SIGN
        )
    }
}