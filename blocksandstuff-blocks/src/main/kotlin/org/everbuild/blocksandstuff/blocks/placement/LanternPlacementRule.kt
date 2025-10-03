package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class LanternPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockBelow = placementState.instance.getBlock(placementState.placePosition.add(0.0, -1.0, 0.0))
        val blockAbove = placementState.instance.getBlock(placementState.placePosition.add(0.0, 1.0, 0.0))
        val canStandOnBlock = canSupport(blockBelow, BlockFace.TOP)
        val canHangFromBlock = canSupport(blockAbove, BlockFace.BOTTOM)
        return when {
            canHangFromBlock && (placementState.blockFace == BlockFace.BOTTOM) -> {
                placementState.block.withProperty("hanging", "true")
            }

            canStandOnBlock -> {
                placementState.block.withProperty("hanging", "false")
            }

            canHangFromBlock -> {
                placementState.block.withProperty("hanging", "true")
            }

            else -> null
        }
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val currentBlock = updateState.currentBlock
        val isHanging = currentBlock.getProperty("hanging")?.equals("true") ?: false
        val supportBlock = if (isHanging) {
            updateState.instance.getBlock(updateState.blockPosition.add(0.0, 1.0, 0.0))
        } else {
            updateState.instance.getBlock(updateState.blockPosition.add(0.0, -1.0, 0.0))
        }

        val requiredFace = if (isHanging) BlockFace.BOTTOM else BlockFace.TOP
        if (!canSupport(supportBlock, requiredFace)) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return currentBlock
    }

    private fun canSupport(supportBlock: Block, requiredFace: BlockFace): Boolean {
        if (supportBlock.registry()!!.collisionShape().isFaceFull(requiredFace)) {
            return true
        }
        return when (supportBlock.name()) {
            "minecraft:chain" -> {
                supportBlock.getProperty("axis") == "y"
            }

            "minecraft:iron_bars" -> true
            else -> {
                supportBlock.name().contains("glass_pane")
            }
        }
    }

    companion object {
        private val SPECIAL_SUPPORT_BLOCKS = setOf(
            "minecraft:chain",
            "minecraft:iron_bars"
        )

        private fun isSpecialSupportBlock(blockName: String): Boolean {
            return SPECIAL_SUPPORT_BLOCKS.contains(blockName) || blockName.contains("glass_pane")
        }
    }
}