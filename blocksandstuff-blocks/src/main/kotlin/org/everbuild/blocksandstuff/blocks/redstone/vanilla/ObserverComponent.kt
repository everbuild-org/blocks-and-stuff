package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Observer. Emits a one-tick pulse through its back when the watched block (the one it faces)
 * changes state.
 */
class ObserverComponent : RedstoneComponent {
    private var lastWatched: Block? = null
    private var pulsing = false
    private var pulseAt = 0L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return ComponentEvaluation.None
        val watched = ctx.block(ctx.position.relative(facing))
        val now = ctx.tick()

        if (pulsing && now >= pulseAt) {
            pulsing = false
            return ComponentEvaluation.SetState(block.withProperty("powered", "false"))
        }

        val previous = lastWatched
        lastWatched = watched
        if (previous == null) return ComponentEvaluation.None

        if (!pulsing && changed(previous, watched)) {
            pulsing = true
            pulseAt = now + 1
            ctx.scheduleTick(1)
            return ComponentEvaluation.SetState(block.withProperty("powered", "true"))
        }

        return ComponentEvaluation.None
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit =
        if (Facing.of(ctx.block())?.oppositeFace == face && ctx.block().getProperty("powered") == "true") {
            Emit(REDSTONE_MAX_POWER, REDSTONE_MAX_POWER)
        } else {
            Emit.ZERO
        }

    private fun changed(
        a: Block,
        b: Block,
    ): Boolean = a.stateId() != b.stateId() || a.nbt() != b.nbt()
}
