package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler

class CandlePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val positionBelow = placementState.placePosition.sub(0.0, 1.0, 0.0)
        val blockBelow = placementState.instance.getBlock(positionBelow)
        if (blockBelow.compare(Block.CAKE, Block.Comparator.ID)) {
            (placementState.instance as Instance).setBlock(
                positionBelow,
                CANDLE_CAKE[block]?.withDefaultHandler() ?: return null
            )
            return Block.AIR
        }

        if (!blockBelow.registry()!!.collisionShape().isFaceFull(BlockFace.TOP)) return null

        val oldBlock = placementState.instance.getBlock(placementState.placePosition)
        if (!oldBlock.compare(block, Block.Comparator.ID)) {
            return if (oldBlock.isWater()) {
                block.withProperty("waterlogged", "true")
            } else {
                block
            }
        }

        val oldCandles = oldBlock.getProperty("candles")?.toIntOrNull() ?: 0
        return oldBlock.withProperty("candles", oldCandles.inc().toString())
    }

    override fun isSelfReplaceable(replacement: Replacement): Boolean {
        val candles = replacement.block.getProperty("candles")?.toIntOrNull() ?: return false
        return candles < 4
    }

    companion object {
        val CANDLE_CAKE = mapOf(
            Block.CANDLE to Block.CANDLE_CAKE,
            Block.WHITE_CANDLE to Block.WHITE_CANDLE_CAKE,
            Block.ORANGE_CANDLE to Block.ORANGE_CANDLE_CAKE,
            Block.MAGENTA_CANDLE to Block.MAGENTA_CANDLE_CAKE,
            Block.LIGHT_BLUE_CANDLE to Block.LIGHT_BLUE_CANDLE_CAKE,
            Block.YELLOW_CANDLE to Block.YELLOW_CANDLE_CAKE,
            Block.LIME_CANDLE to Block.LIME_CANDLE_CAKE,
            Block.PINK_CANDLE to Block.PINK_CANDLE_CAKE,
            Block.GRAY_CANDLE to Block.GRAY_CANDLE_CAKE,
            Block.LIGHT_GRAY_CANDLE to Block.LIGHT_GRAY_CANDLE_CAKE,
            Block.CYAN_CANDLE to Block.CYAN_CANDLE_CAKE,
            Block.PURPLE_CANDLE to Block.PURPLE_CANDLE_CAKE,
            Block.BLUE_CANDLE to Block.BLUE_CANDLE_CAKE,
            Block.BROWN_CANDLE to Block.BROWN_CANDLE_CAKE,
            Block.GREEN_CANDLE to Block.GREEN_CANDLE_CAKE,
            Block.RED_CANDLE to Block.RED_CANDLE_CAKE,
            Block.BLACK_CANDLE to Block.BLACK_CANDLE_CAKE
        )
    }
}