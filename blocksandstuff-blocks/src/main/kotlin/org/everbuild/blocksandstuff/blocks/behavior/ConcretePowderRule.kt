package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.common.utils.isWater
import java.util.concurrent.ThreadLocalRandom

class ConcretePowderRule(val block: Block) : BlockHandler {
    override fun getKey(): Key = Key.key("blocksandstuff:concrete_powder_behavior")

    override fun onPlace(placement: BlockHandler.Placement) {
        if (touchesWater(placement.instance, placement.blockPosition)) {
            val solidBlock = solidify(placement.block)
            placement.instance.scheduler().scheduleNextTick {
                placement.instance.setBlock(placement.blockPosition, solidBlock)
            }
        }
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        checkNeighboringConcretePowder(destroy.instance, destroy.blockPosition)
    }

    override fun tick(tick: BlockHandler.Tick) {
        if (ThreadLocalRandom.current().nextInt(40) == 0) {
            if (touchesWater(tick.instance, tick.blockPosition)) {
                val solidBlock = solidify(tick.block)
                tick.instance.setBlock(tick.blockPosition, solidBlock)
            }
        }
    }

    override fun isTickable(): Boolean {
        return true
    }

    private fun touchesWater(instance: Instance, position: Point): Boolean {
        val directions = arrayOf(
            intArrayOf(1, 0, 0),   // Osten
            intArrayOf(-1, 0, 0),  // Westen
            intArrayOf(0, 1, 0),   // Oben
            intArrayOf(0, -1, 0),  // Unten
            intArrayOf(0, 0, 1),   // Süden
            intArrayOf(0, 0, -1)   // Norden
        )
        
        for (dir in directions) {
            val neighborPos = position.add(dir[0].toDouble(), dir[1].toDouble(), dir[2].toDouble())
            val neighborBlock = instance.getBlock(neighborPos)
            if (neighborBlock.isWater()) {
                return true
            }
        }
        return false
    }



    private fun checkNeighboringConcretePowder(instance: Instance, position: Point) {
        val directions = arrayOf(
            intArrayOf(1, 0, 0),   // Osten
            intArrayOf(-1, 0, 0),  // Westen
            intArrayOf(0, 1, 0),   // Oben
            intArrayOf(0, -1, 0),  // Unten
            intArrayOf(0, 0, 1),   // Süden
            intArrayOf(0, 0, -1)   // Norden
        )
        
        for (dir in directions) {
            val neighborPos = position.add(dir[0].toDouble(), dir[1].toDouble(), dir[2].toDouble())
            val neighborBlock = instance.getBlock(neighborPos)
            if (neighborBlock.name().endsWith("_concrete_powder")) {
                if (touchesWater(instance, neighborPos)) {
                    val solidBlock = solidify(neighborBlock)
                    instance.setBlock(neighborPos, solidBlock)
                }
            }
        }
    }

    private fun solidify(powderBlock: Block): Block {
        val powderName = powderBlock.name()
        val concreteName = powderName.replace("_concrete_powder", "_concrete")
        val concreteBlock = Block.fromKey(concreteName)
        return concreteBlock ?: powderBlock
    }
}