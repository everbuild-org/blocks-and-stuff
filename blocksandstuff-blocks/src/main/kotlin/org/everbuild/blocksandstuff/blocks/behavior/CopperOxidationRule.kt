package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import java.util.concurrent.ThreadLocalRandom
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.EventDispatcher
import org.everbuild.blocksandstuff.blocks.event.CopperOxidationEvent
import org.everbuild.blocksandstuff.blocks.randomticking.RandomTickHandler

class CopperOxidationRule(private val block: Block) : BlockHandler, RandomTickHandler {
    override fun getKey(): Key = block.key()

    fun getNextOxidationStage(): Block? {
        return oxidationStages[block]
    }

    override fun onRandomTick(randomTick: RandomTickHandler.RandomTick): Block? {
        val instance = randomTick.instance
        val blockPosition = randomTick.blockPosition
        val exposedToAir = countExposedSides(instance, blockPosition) > 0

        if (exposedToAir && ThreadLocalRandom.current().nextInt(100) < 5) {
            return oxidizeBlock(instance, blockPosition, randomTick.block)
        }

        return null
    }

    private fun countExposedSides(instance: Instance, blockPosition: Point): Int {
        var exposedSides = 0

        for (face in BlockFace.entries) {
            val neighborPos = blockPosition.add(
                face.toDirection().vec()
            )
            val neighborBlock = instance.getBlock(neighborPos)
            if (neighborBlock == Block.AIR) {
                exposedSides++
            }
        }
        return exposedSides
    }

    private fun oxidizeBlock(instance: Instance, blockPosition: Point, block: Block): Block? {
        var nextStage = getNextOxidationStage() ?: return null
        val handler = MinecraftServer.getBlockManager().getHandler(nextStage.key().asString())
        if (handler != null) {
            nextStage = nextStage.withHandler(handler)
        }
        val event = CopperOxidationEvent(
            block,
            nextStage,
            BlockVec(blockPosition),
            instance
        )
        EventDispatcher.call(event)
        if (event.isCancelled) return null
        return nextStage
    }

    companion object {
        private val oxidationStages = mapOf(
            Block.COPPER_BLOCK to Block.EXPOSED_COPPER,
            Block.EXPOSED_COPPER to Block.WEATHERED_COPPER,
            Block.WEATHERED_COPPER to Block.OXIDIZED_COPPER,

            Block.CUT_COPPER to Block.EXPOSED_CUT_COPPER,
            Block.EXPOSED_CUT_COPPER to Block.WEATHERED_CUT_COPPER,
            Block.WEATHERED_CUT_COPPER to Block.OXIDIZED_CUT_COPPER,

            Block.CUT_COPPER_STAIRS to Block.EXPOSED_CUT_COPPER_STAIRS,
            Block.EXPOSED_CUT_COPPER_STAIRS to Block.WEATHERED_CUT_COPPER_STAIRS,
            Block.WEATHERED_CUT_COPPER_STAIRS to Block.OXIDIZED_CUT_COPPER_STAIRS,

            Block.CUT_COPPER_SLAB to Block.EXPOSED_CUT_COPPER_SLAB,
            Block.EXPOSED_CUT_COPPER_SLAB to Block.WEATHERED_CUT_COPPER_SLAB,
            Block.WEATHERED_CUT_COPPER_SLAB to Block.OXIDIZED_CUT_COPPER_SLAB,

            Block.CHISELED_COPPER to Block.EXPOSED_CHISELED_COPPER,
            Block.EXPOSED_CHISELED_COPPER to Block.WEATHERED_CHISELED_COPPER,
            Block.WEATHERED_CHISELED_COPPER to Block.OXIDIZED_CHISELED_COPPER,

            Block.COPPER_GRATE to Block.EXPOSED_COPPER_GRATE,
            Block.EXPOSED_COPPER_GRATE to Block.WEATHERED_COPPER_GRATE,
            Block.WEATHERED_COPPER_GRATE to Block.OXIDIZED_COPPER_GRATE,

            Block.COPPER_BULB to Block.EXPOSED_COPPER_BULB,
            Block.EXPOSED_COPPER_BULB to Block.WEATHERED_COPPER_BULB,
            Block.WEATHERED_COPPER_BULB to Block.OXIDIZED_COPPER_BULB,
        )

        fun getOxidizableBlocks(): List<Block> {
            return oxidationStages.keys.toList()
        }
    }
}