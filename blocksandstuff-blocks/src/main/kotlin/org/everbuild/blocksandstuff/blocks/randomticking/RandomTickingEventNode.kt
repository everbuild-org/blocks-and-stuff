package org.everbuild.blocksandstuff.blocks.randomticking

import kotlin.random.Random
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider

private fun Pair<Int, Int>.around(): List<Pair<Int, Int>> {
    val (x, y) = this
    return buildList {
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx != 0 || dy != 0) {
                    add((x + dx) to (y + dy))
                }
            }
        }
    }
}

private val random: Random = Random.Default
private val airId = Block.AIR.stateId()

fun getRandomTickingEventNode() = EventNode.type("random-ticking", EventFilter.INSTANCE)
    .addListener(InstanceTickEvent::class.java) { event ->
        val chunks = event.instance.chunks.map { it.chunkX to it.chunkZ }.toSet()
        val randomTickSpeed = InstanceOptionsProvider.getForInstance(event.instance).randomTickSpeed
        for (chunk in event.instance.chunks) {
            if ((chunk.chunkX to chunk.chunkZ).around().all { chunks.contains(it) }) continue

            val minChunkX = chunk.chunkX * 16
            val minChunkZ = chunk.chunkZ * 16
            for (index in chunk.sections.indices) {
                val section = chunk.sections[index] ?: continue
                val minSectionPos = (chunk.minSection + index) * 16

                repeat(randomTickSpeed) {
                    val random = random.nextLong()
                    val xRandom = (random and 0xF).toInt()
                    val yRandom = (random shr 4 and 0xF).toInt()
                    val zRandom = (random shr 8 and 0xF).toInt()
                    val blockId = section.blockPalette().get(xRandom, yRandom, zRandom)
                    if (blockId == airId) return@repeat
                    val block = Block.fromBlockId(blockId) ?: return@repeat
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
                    chunk.setBlock(pos, newBlock)
                }
            }
        }
    }