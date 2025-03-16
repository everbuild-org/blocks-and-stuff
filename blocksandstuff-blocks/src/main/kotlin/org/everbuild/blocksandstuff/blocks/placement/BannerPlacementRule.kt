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

        val wallBanner = when (placementState.block.registry().material()) {
            Block.ORANGE_BANNER.registry().material() -> Block.ORANGE_WALL_BANNER
            Block.MAGENTA_BANNER.registry().material() -> Block.MAGENTA_WALL_BANNER
            Block.LIGHT_BLUE_BANNER.registry().material() -> Block.LIGHT_BLUE_WALL_BANNER
            Block.YELLOW_BANNER.registry().material() -> Block.YELLOW_WALL_BANNER
            Block.LIME_BANNER.registry().material() -> Block.LIME_WALL_BANNER
            Block.PINK_BANNER.registry().material() -> Block.PINK_WALL_BANNER
            Block.GRAY_BANNER.registry().material() -> Block.GRAY_WALL_BANNER
            Block.LIGHT_GRAY_BANNER.registry().material() -> Block.LIGHT_GRAY_WALL_BANNER
            Block.CYAN_BANNER.registry().material() -> Block.CYAN_WALL_BANNER
            Block.PURPLE_BANNER.registry().material() -> Block.PURPLE_WALL_BANNER
            Block.BLUE_BANNER.registry().material() -> Block.BLUE_WALL_BANNER
            Block.BROWN_BANNER.registry().material() -> Block.BROWN_WALL_BANNER
            Block.GREEN_BANNER.registry().material() -> Block.GREEN_WALL_BANNER
            Block.RED_BANNER.registry().material() -> Block.RED_WALL_BANNER
            Block.BLACK_BANNER.registry().material() -> Block.BLACK_WALL_BANNER
            Block.WHITE_BANNER.registry().material() -> Block.WHITE_WALL_BANNER
            else -> return null
        }

        return wallBanner.withNbt(placementState.block.nbtOrEmpty())
            .withProperty("facing", placementState.blockFace!!.name.lowercase())
    }
}