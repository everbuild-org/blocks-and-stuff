package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.blocks.randomticking.RandomTickHandler
import org.everbuild.blocksandstuff.common.utils.isWater
import org.everbuild.blocksandstuff.common.utils.withDefaultHandler

class CoralRule(private val block: Block): BlockHandler, RandomTickHandler {
    override fun getKey(): Key = block.key()

    override fun onRandomTick(randomTick: RandomTickHandler.RandomTick): Block? {
        val instance = randomTick.instance
        val pos = randomTick.blockPosition
        val current = randomTick.block
        val isCoralBlock = current.defaultState() in CORAL_BLOCKS

        if (!isCoralBlock) {
            if (current.getProperty("waterlogged") == "true") return null
        } else {
            var hasAdjacentWater = false
            for (face in BlockFace.entries) {
                val neighbor = instance.getBlock(pos.add(face.toDirection().vec()))
                if (neighbor.isWater() || neighbor.getProperty("waterlogged") == "true") {
                    hasAdjacentWater = true
                    break
                }
            }
            if (hasAdjacentWater) return null
        }

        val dead = deadVariantOf(current) ?: return null
        var result = dead.withDefaultHandler()
        current.getProperty("facing")?.let { facing ->
            result = result.withProperty("facing", facing)
        }
        if (result.getProperty("waterlogged") != null) {
            result = result.withProperty("waterlogged", "false")
        }
        return result
    }

    private fun deadVariantOf(block: Block): Block? {
        return DEAD_VARIANTS[block.defaultState()]
    }

    companion object {
        private val CORAL_BLOCKS = setOf(
            Block.TUBE_CORAL_BLOCK,
            Block.BRAIN_CORAL_BLOCK,
            Block.BUBBLE_CORAL_BLOCK,
            Block.FIRE_CORAL_BLOCK,
            Block.HORN_CORAL_BLOCK
        )

        private val DEAD_VARIANTS = mapOf(
            // Pflanzen (stehende Korallen)
            Block.TUBE_CORAL to Block.DEAD_TUBE_CORAL,
            Block.BRAIN_CORAL to Block.DEAD_BRAIN_CORAL,
            Block.BUBBLE_CORAL to Block.DEAD_BUBBLE_CORAL,
            Block.FIRE_CORAL to Block.DEAD_FIRE_CORAL,
            Block.HORN_CORAL to Block.DEAD_HORN_CORAL,

            // Fächer am Boden
            Block.TUBE_CORAL_FAN to Block.DEAD_TUBE_CORAL_FAN,
            Block.BRAIN_CORAL_FAN to Block.DEAD_BRAIN_CORAL_FAN,
            Block.BUBBLE_CORAL_FAN to Block.DEAD_BUBBLE_CORAL_FAN,
            Block.FIRE_CORAL_FAN to Block.DEAD_FIRE_CORAL_FAN,
            Block.HORN_CORAL_FAN to Block.DEAD_HORN_CORAL_FAN,

            // Wandfächer
            Block.TUBE_CORAL_WALL_FAN to Block.DEAD_TUBE_CORAL_WALL_FAN,
            Block.BRAIN_CORAL_WALL_FAN to Block.DEAD_BRAIN_CORAL_WALL_FAN,
            Block.BUBBLE_CORAL_WALL_FAN to Block.DEAD_BUBBLE_CORAL_WALL_FAN,
            Block.FIRE_CORAL_WALL_FAN to Block.DEAD_FIRE_CORAL_WALL_FAN,
            Block.HORN_CORAL_WALL_FAN to Block.DEAD_HORN_CORAL_WALL_FAN,

            // Coral Blocks
            Block.TUBE_CORAL_BLOCK to Block.DEAD_TUBE_CORAL_BLOCK,
            Block.BRAIN_CORAL_BLOCK to Block.DEAD_BRAIN_CORAL_BLOCK,
            Block.BUBBLE_CORAL_BLOCK to Block.DEAD_BUBBLE_CORAL_BLOCK,
            Block.FIRE_CORAL_BLOCK to Block.DEAD_FIRE_CORAL_BLOCK,
            Block.HORN_CORAL_BLOCK to Block.DEAD_HORN_CORAL_BLOCK,
        )
    }
}
