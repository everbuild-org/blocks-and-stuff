package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class AttachedStemPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockUpdate(updateState: UpdateState): Block {
        val current = updateState.currentBlock

        if (!updateState.instance.getBlock(updateState.blockPosition().add(0.0, -1.0, 0.0)).compare(Block.FARMLAND)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        val normalStem = NORMAL_STEMS[getBlock()] ?: return current
        val fruit = FRUITS[getBlock()] ?: return current
        val facing = current.getProperty("facing")?.let { BlockFace.valueOf(it.uppercase()).toDirection() } ?: return current
        val fruitBlock = updateState.instance.getBlock(updateState.blockPosition().add(facing.vec()))

        if (!fruitBlock.compare(fruit)) {
            val handler = MinecraftServer.getBlockManager().getHandler(normalStem.key().asString())
            val reverted = normalStem.withProperty("age", "7")
            return if (handler != null) reverted.withHandler(handler) else reverted
        }

        return current
    }

    override fun blockPlace(placementState: PlacementState): Block = placementState.block

    private companion object {
        private val NORMAL_STEMS =
            mapOf(
                Block.ATTACHED_PUMPKIN_STEM to Block.PUMPKIN_STEM,
                Block.ATTACHED_MELON_STEM to Block.MELON_STEM,
            )
        private val FRUITS =
            mapOf(
                Block.ATTACHED_PUMPKIN_STEM to Block.PUMPKIN,
                Block.ATTACHED_MELON_STEM to Block.MELON,
            )
    }
}
