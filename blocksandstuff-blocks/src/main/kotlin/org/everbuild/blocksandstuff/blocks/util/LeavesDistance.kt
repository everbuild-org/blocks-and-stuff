package org.everbuild.blocksandstuff.blocks.util

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.tag.BlockTags
import java.util.ArrayDeque
import java.util.HashSet

const val LEAVES_DECAY_DISTANCE: Int = 7

private val LEAF_KEYS: Set<String> =
    BlockTags.getTaggedWith("minecraft:leaves").mapTo(HashSet()) { it.key().asString() }
private val LOG_KEYS: Set<String> =
    BlockTags.getTaggedWith("minecraft:logs").mapTo(HashSet()) { it.key().asString() }

/**
 * Vanilla leaf distance: the number of leaf steps to the nearest log, capped at
 * [LEAVES_DECAY_DISTANCE]. A value of [LEAVES_DECAY_DISTANCE] means "no log within reach".
 */
fun leavesDistance(
    instance: Block.Getter,
    origin: Point,
): Int {
    val visited = HashSet<Point>()
    val queue = ArrayDeque<Pair<Point, Int>>()
    visited.add(origin)
    queue.add(origin to 0)

    while (queue.isNotEmpty()) {
        val (position, distance) = queue.removeFirst()
        if (distance >= LEAVES_DECAY_DISTANCE) continue

        for (face in BlockFace.entries) {
            val neighbor = position.relative(face)
            if (!visited.add(neighbor)) continue
            if (instance is Instance && !instance.isChunkLoaded(neighbor)) continue

            val key = instance.getBlock(neighbor).key().asString()
            if (key in LOG_KEYS) return distance + 1
            if (key in LEAF_KEYS) queue.add(neighbor to distance + 1)
        }
    }
    return LEAVES_DECAY_DISTANCE
}
