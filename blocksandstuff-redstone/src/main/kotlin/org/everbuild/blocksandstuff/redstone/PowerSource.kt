package org.everbuild.blocksandstuff.redstone

import net.minestom.server.instance.block.Block

/** A constant source of power (redstone block, lever, ...). */
interface PowerSource {
    /** Signal strength emitted while active (0 disables the source). */
    fun strength(block: Block): Int

    /** When `true`, power is only emitted through the `facing` side. */
    fun directional(block: Block): Boolean = false
}
