package org.everbuild.blocksandstuff.blocks.randomticking

import kotlin.random.Random
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceChunkUnloadEvent
import net.minestom.server.event.instance.InstanceRegisterEvent
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider
import org.everbuild.blocksandstuff.common.utils.SimpleInvalidatableCache

private fun Chunk.around(): List<Pair<Int, Int>> {
    return buildList {
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx != 0 || dy != 0) {
                    add((chunkX + dx) to (chunkZ + dy))
                }
            }
        }
    }
}

private val random: Random = Random.Default

private val innerChunkCache = SimpleInvalidatableCache<Instance, Set<Chunk>> { instance ->
    val chunkCoordinates = instance.chunks.asSequence().map { it.chunkX to it.chunkZ }.toSet()
    return@SimpleInvalidatableCache instance.chunks.filter { chunk ->
        chunk.around().all { neighbor -> chunkCoordinates.contains(neighbor) }
    }.toSet()
}

fun getRandomTickingEventNode() = EventNode.type("random-ticking", EventFilter.INSTANCE)
    .addListener(InstanceChunkLoadEvent::class.java) { event ->
        innerChunkCache.invalidate(event.instance)
    }
    .addListener(InstanceChunkUnloadEvent::class.java) { event ->
        innerChunkCache.invalidate(event.instance)
    }
    .addListener(InstanceRegisterEvent::class.java) { event ->
        innerChunkCache.invalidate(event.instance)
    }
    .addListener(InstanceTickEvent::class.java) { event ->
        val randomTickSpeed = InstanceOptionsProvider.getForInstance(event.instance).randomTickSpeed
        for (chunk in innerChunkCache[event.instance]) {
            val minChunkX = chunk.chunkX * 16
            val minChunkZ = chunk.chunkZ * 16
            for (index in chunk.sections.indices) {
                val minSectionPos = (chunk.minSection + index) * 16

                repeat(randomTickSpeed) {
                    val randomValue = random.nextLong()
                    val xRandom = (randomValue and 0xF).toInt()
                    val yRandom = (randomValue shr 4 and 0xF).toInt()
                    val zRandom = (randomValue shr 8 and 0xF).toInt()
                    val block = chunk.getBlock(
                        xRandom,
                        yRandom + minSectionPos,
                        zRandom,
                        Block.Getter.Condition.CACHED
                    ) ?: return@repeat
                    val handler = block.handler() ?: return@repeat
                    if (handler !is RandomTickHandler) return@repeat
                    val pos = BlockVec(xRandom + minChunkX, yRandom + minSectionPos, zRandom + minChunkZ)
                    val newBlock = handler.onRandomTick(
                        RandomTickHandler.RandomTick(
                            block,
                            event.instance,
                            pos,
                        )
                    ) ?: return@repeat
                    event.instance.setBlock(pos, newBlock)
                }
            }
        }
    }