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
        if (strength <= 0) return Emit.ZERO
        return if (face == supportFace(block)) Emit(strength, strength) else Emit(strength, 0)
    }

    /** Direction from the switch to the block it is attached to; only that block is strongly powered. */
    private fun supportFace(block: Block): BlockFace? =
        when (block.getProperty("face") ?: "floor") {
            "floor" -> BlockFace.BOTTOM
            "ceiling" -> BlockFace.TOP
            "wall" -> block.getProperty("facing")?.let { BlockFace.valueOf(it.uppercase()).oppositeFace }
            else -> null
        }
}
