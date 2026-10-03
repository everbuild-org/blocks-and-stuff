package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.sixteenStepRotation

class BannerPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        if (placementState.blockFace == null || placementState.blockFace == BlockFace.BOTTOM) return null

        if (placementState.blockFace == BlockFace.TOP) {
            return block.withProperty("rotation", ((placementState.sixteenStepRotation() + 8) % 16).toString())
        }

        val wallBanner = when (placementState.block.material()) {
            Block.ORANGE_BANNER.material() -> Block.ORANGE_WALL_BANNER
            Block.MAGENTA_BANNER.material() -> Block.MAGENTA_WALL_BANNER
            Block.LIGHT_BLUE_BANNER.material() -> Block.LIGHT_BLUE_WALL_BANNER
            Block.YELLOW_BANNER.material() -> Block.YELLOW_WALL_BANNER
            Block.LIME_BANNER.material() -> Block.LIME_WALL_BANNER
            Block.PINK_BANNER.material() -> Block.PINK_WALL_BANNER
            Block.GRAY_BANNER.material() -> Block.GRAY_WALL_BANNER
            Block.LIGHT_GRAY_BANNER.material() -> Block.LIGHT_GRAY_WALL_BANNER
            Block.CYAN_BANNER.material() -> Block.CYAN_WALL_BANNER
            Block.PURPLE_BANNER.material() -> Block.PURPLE_WALL_BANNER
            Block.BLUE_BANNER.material() -> Block.BLUE_WALL_BANNER
            Block.BROWN_BANNER.material() -> Block.BROWN_WALL_BANNER
            Block.GREEN_BANNER.material() -> Block.GREEN_WALL_BANNER
            Block.RED_BANNER.material() -> Block.RED_WALL_BANNER
            Block.BLACK_BANNER.material() -> Block.BLACK_WALL_BANNER
            Block.WHITE_BANNER.material() -> Block.WHITE_WALL_BANNER
            else -> return null
        }

        return wallBanner.withNbt(placementState.block.nbtOrEmpty())
            .withProperty("facing", placementState.blockFace!!.name.lowercase())
    }
}