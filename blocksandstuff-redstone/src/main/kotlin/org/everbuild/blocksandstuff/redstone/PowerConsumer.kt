package org.everbuild.blocksandstuff.redstone

import net.minestom.server.instance.block.Block

/**
 * A block that reacts to whether it is powered.
 *
 * Called only when the powered state changes. Returns the replacement block to write, or `null` to
 * leave the block untouched.
 */
fun interface PowerConsumer {
    fun onPoweredChanged(
        ctx: UpdateContext,
        powered: Boolean,
        strength: Int,
    ): Block?
}
