package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent

/**
 * Lever, button and pressure plate. Purely a source: its on/off state is set outside the engine
 * (interaction, timing, tick), the engine only reads it.
 */
class SwitchComponent : RedstoneComponent {
    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit {
        val block = ctx.block()
        val strength = when {
            block.getProperty("powered") == "true" -> REDSTONE_MAX_POWER
            else -> block.getProperty("power")?.toIntOrNull()?.coerceIn(0, REDSTONE_MAX_POWER) ?: 0
        }
        return if (strength > 0) Emit(strength, strength) else Emit.ZERO
    }
}
