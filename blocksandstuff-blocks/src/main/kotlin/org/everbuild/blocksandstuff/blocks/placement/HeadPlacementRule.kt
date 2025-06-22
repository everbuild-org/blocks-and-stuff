package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.sixteenStepRotation
import java.util.*

class HeadPlacementRule(block: Block) : BlockPlacementRule(block) {
    companion object {
        private val WALL_VARIANTS = mapOf(
            Block.PLAYER_HEAD to Block.PLAYER_WALL_HEAD,
            Block.SKELETON_SKULL to Block.SKELETON_WALL_SKULL,
            Block.WITHER_SKELETON_SKULL to Block.WITHER_SKELETON_WALL_SKULL,
            Block.ZOMBIE_HEAD to Block.ZOMBIE_WALL_HEAD,
            Block.CREEPER_HEAD to Block.CREEPER_WALL_HEAD,
            Block.DRAGON_HEAD to Block.DRAGON_WALL_HEAD
        )
    }

    override fun blockPlace(placementState: PlacementState): Block? {
        val clickedFace = placementState.blockFace ?: return block
        return when (clickedFace) {
            BlockFace.TOP, BlockFace.BOTTOM -> {
                val rotation = placementState.sixteenStepRotation()
                block.withProperty("rotation", rotation.toString())
            }
            else -> {
                val wallVariant = WALL_VARIANTS[block]
                    ?: return block
                val facing = clickedFace.name.lowercase(Locale.ROOT)
                wallVariant.withProperty("facing", facing)
            }
        }
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        return updateState.currentBlock
    }
}