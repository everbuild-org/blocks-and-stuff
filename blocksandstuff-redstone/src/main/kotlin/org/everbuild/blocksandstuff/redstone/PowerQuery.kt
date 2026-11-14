package org.everbuild.blocksandstuff.redstone

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.BlockFace

/**
 * Read-only view of the redstone power in a world.
 *
 * Signals are directional: [weakSignal] / [directSignal] describe what the block at [from] emits
 * through [face] (toward `from.relative(face)`). Power is derived, never read back from a
 * persistent block property of a plain block.
 */
interface PowerQuery {
    /** Weak/indirect signal emitted from [from] through [face]. */
    fun weakSignal(from: Point, face: BlockFace): Int

    /** Strong/direct signal emitted from [from] through [face]. */
    fun directSignal(from: Point, face: BlockFace): Int

    /** Highest weak or direct signal arriving at [pos] from any side. */
    fun signalInto(pos: Point): Int =
        BlockFace.entries.maxOf { face ->
            val neighbour = pos.relative(face)
            val into = face.oppositeFace
            maxOf(weakSignal(neighbour, into), directSignal(neighbour, into))
        }

    /** Whether any side delivers a signal into [pos]. */
    fun isPowered(pos: Point): Boolean = signalInto(pos) > 0

    /** Whether any side delivers a strong/direct signal into [pos]. */
    fun isIndirectlyPowered(pos: Point): Boolean =
        BlockFace.entries.any { face -> directSignal(pos.relative(face), face.oppositeFace) > 0 }
}
