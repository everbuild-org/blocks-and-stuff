package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.blocksandstuff.blocks.randomticking.RandomTickHandler
import org.everbuild.blocksandstuff.common.utils.isWater

class FarmlandRule(private val block: Block) : BlockHandler, RandomTickHandler {
    override fun getKey(): Key = block.key()
    private fun hasNearbyWater(instance: Block.Getter, pos: Point): Boolean {
        for (dy in 0..1) {
            for (dx in -4..4) {
                for (dz in -4..4) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val bp = pos.add(dx.toDouble(), dy.toDouble(), dz.toDouble())
                    if (instance.getBlock(bp).isWater()) return true
                }
            }
        }
        return false
    }

    override fun onRandomTick(randomTick: RandomTickHandler.RandomTick): Block {
        val instance = randomTick.instance
        val blockPosition = randomTick.blockPosition
        if (hasNearbyWater(instance, blockPosition)) {
            return randomTick.block.withProperty("moisture", "7")
        }
        var moisture = randomTick.block.getProperty("moisture")?.toIntOrNull() ?: 0
        moisture -= 1
        if (moisture < 0) {
            return Block.DIRT
        }
        return randomTick.block.withProperty("moisture", moisture.toString())
    }
}