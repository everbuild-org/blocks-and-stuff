package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.event.instance.InstanceBlockUpdateEvent

/** Keeps the two halves of a copper door on the same oxidation stage. */
object CopperDoorSync {
    private var enabled = false

    fun enable() {
        if (enabled) return
        enabled = true

        MinecraftServer.getGlobalEventHandler().addListener(InstanceBlockUpdateEvent::class.java) { event ->
            val block = event.block
            if (!block.key().value().contains("copper_door")) return@addListener
            val half = block.getProperty("half") ?: return@addListener

            val otherPosition =
                if (half == "lower") {
                    event.blockPosition.add(0.0, 1.0, 0.0)
                } else {
                    event.blockPosition.sub(0.0, 1.0, 0.0)
                }
            val other = event.instance.getBlock(otherPosition)
            if (!other.key().value().contains("copper_door")) return@addListener

            val desired = block.withProperty("half", if (half == "lower") "upper" else "lower")
            if (other != desired) {
                event.instance.setBlock(otherPosition, desired, false)
            }
        }
    }
}
