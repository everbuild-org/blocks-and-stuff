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
        println("ConcretePowderRule: Block placed - ${placement.block.name()}")
        if (touchesWater(placement.instance, placement.blockPosition)) {
            println("ConcretePowderRule: Water detected during placement, converting to concrete")
            val solidBlock = solidify(placement.block)
            placement.instance.setBlock(placement.blockPosition, solidBlock)
            println("ConcretePowderRule: Converted to - ${solidBlock.name()}")
        } else {
            println("ConcretePowderRule: No water detected during placement")
        }
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        println("ConcretePowderRule: Block destroyed, checking neighbors")
        checkNeighboringConcretePowder(destroy.instance, destroy.blockPosition)
    }

    override fun tick(tick: BlockHandler.Tick) {
        // Prüfe gelegentlich (etwa alle 1-2 Sekunden) ob Wasser in der Nähe ist
        if (ThreadLocalRandom.current().nextInt(40) == 0) {
            println("ConcretePowderRule: Checking for water during tick - ${tick.block.name()}")
            if (touchesWater(tick.instance, tick.blockPosition)) {
                println("ConcretePowderRule: Water detected during tick, converting to concrete")
                val solidBlock = solidify(tick.block)
                tick.instance.setBlock(tick.blockPosition, solidBlock)
                println("ConcretePowderRule: Converted to - ${solidBlock.name()}")
            }
        }
    }

    override fun isTickable(): Boolean {
        return true
    }

    private fun touchesWater(instance: Instance, position: Point): Boolean {
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue
                    }
                    val neighborPos = position.add(dx.toDouble(), dy.toDouble(), dz.toDouble())
                    val neighborBlock = instance.getBlock(neighborPos)
                    
                    println("ConcretePowderRule: Checking block ${neighborBlock.name()} - isWater: ${neighborBlock.isWater()}, waterlogged: ${isWaterlogged(neighborBlock)}")
                    
                    if (neighborBlock.isWater()) {
                        println("ConcretePowderRule: Found water at $neighborPos - ${neighborBlock.name()}")
                        return true
                    }
                    if (isWaterlogged(neighborBlock)) {
                        println("ConcretePowderRule: Found waterlogged block at $neighborPos - ${neighborBlock.name()}")
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isWaterlogged(block: Block): Boolean {
        return try {
            block.getProperty("waterlogged") == "true"
        } catch (e: Exception) {
            false
        }
    }

    private fun checkNeighboringConcretePowder(instance: Instance, position: Point) {
        // Prüfe alle benachbarten Blöcke ob sie Concrete Powder sind und von Wasser berührt werden
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue
                    }
                    val neighborPos = position.add(dx.toDouble(), dy.toDouble(), dz.toDouble())
                    val neighborBlock = instance.getBlock(neighborPos)
                    
                    if (neighborBlock.name().endsWith("_concrete_powder")) {
                        println("ConcretePowderRule: Found neighboring concrete powder: ${neighborBlock.name()}")
                        if (touchesWater(instance, neighborPos)) {
                            val solidBlock = solidify(neighborBlock)
                            instance.setBlock(neighborPos, solidBlock)
                            println("ConcretePowderRule: Converted neighboring powder to - ${solidBlock.name()}")
                        }
                    }
                }
            }
        }
    }

    private fun solidify(powderBlock: Block): Block {
        val powderName = powderBlock.name()
        val concreteName = powderName.replace("_concrete_powder", "_concrete")
        println("ConcretePowderRule: Converting $powderName to $concreteName")
        val concreteBlock = Block.fromKey(concreteName)
        return concreteBlock ?: powderBlock
    }
}