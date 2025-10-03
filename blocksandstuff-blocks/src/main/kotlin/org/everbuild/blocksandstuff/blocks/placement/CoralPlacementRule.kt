package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler

class CoralPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val waterlogged = placementState.instance().getBlock(placementState.placePosition).isWater().toString()
        if (placementState.blockFace!!.toDirection()
                .horizontal() && placementState.instance.getBlock(placementState.placePosition.relative(placementState.blockFace!!.oppositeFace))
                .registry()!!.collisionShape().isFaceFull(placementState.blockFace!!)
        ) {
            WALL_CORALS[block]?.let {
                return it
                    .withDefaultHandler()
                    .withProperty("facing", placementState.blockFace!!.toDirection().toString().lowercase())
                    .withProperty("waterlogged", waterlogged)
            }
        }

        if (!placementState.instance.getBlock(placementState.placePosition.relative(BlockFace.BOTTOM)).registry()
                !!.collisionShape().isFaceFull(
                BlockFace.TOP
            )
        ) return null

        return placementState.block.withProperty("waterlogged", waterlogged)
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        if (!updateState.instance.getBlock(updateState.blockPosition.relative(BlockFace.BOTTOM)).registry()
                !!.collisionShape().isFaceFull(BlockFace.TOP)
        ) {
            return if (updateState.currentBlock.getProperty("waterlogged") == "true") Block.WATER else Block.AIR
        }

        return super.blockUpdate(updateState)
    }

    companion object {
        val WALL_CORALS = mapOf(
            Block.TUBE_CORAL_FAN to Block.TUBE_CORAL_WALL_FAN,
            Block.BRAIN_CORAL_FAN to Block.BRAIN_CORAL_WALL_FAN,
            Block.BUBBLE_CORAL_FAN to Block.BUBBLE_CORAL_WALL_FAN,
            Block.FIRE_CORAL_FAN to Block.FIRE_CORAL_WALL_FAN,
            Block.HORN_CORAL_FAN to Block.HORN_CORAL_WALL_FAN,
            Block.DEAD_TUBE_CORAL_FAN to Block.DEAD_TUBE_CORAL_WALL_FAN,
            Block.DEAD_BRAIN_CORAL_FAN to Block.DEAD_BRAIN_CORAL_WALL_FAN,
            Block.DEAD_BUBBLE_CORAL_FAN to Block.DEAD_BUBBLE_CORAL_WALL_FAN,
            Block.DEAD_FIRE_CORAL_FAN to Block.DEAD_FIRE_CORAL_WALL_FAN,
            Block.DEAD_HORN_CORAL_FAN to Block.DEAD_HORN_CORAL_WALL_FAN
        )
    }
}