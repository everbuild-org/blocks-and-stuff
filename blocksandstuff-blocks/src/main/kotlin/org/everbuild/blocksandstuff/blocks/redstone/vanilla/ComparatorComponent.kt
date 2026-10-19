package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Redstone comparator. Rear and side inputs are analog (0..15). In `compare` mode the rear signal is
 * passed through when `rear >= side`, otherwise the output is 0; in `subtract` mode the output is
 * `rear - side`. The analog value is derived and kept in memory, not persisted.
 */
class ComparatorComponent : RedstoneComponent {
    private var output = 0

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return ComponentEvaluation.None

        // A diode's `facing` points at its input side; the output is the opposite face.
        val rear = inputFrom(ctx, facing, facing.oppositeFace)
        val side = Facing.sides(facing).maxOf { s -> inputFrom(ctx, s, s.oppositeFace) }
        val value =
            if (block.getProperty("mode") == "subtract") {
                (rear - side).coerceAtLeast(0)
            } else {
                if (rear >= side) rear else 0
            }

        val powered = value > 0
        if (value == output && (block.getProperty("powered") == "true") == powered) return ComponentEvaluation.None

        output = value
        return ComponentEvaluation.SetState(block.withProperty("powered", powered.toString()))
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit =
        if (Facing.of(ctx.block())?.oppositeFace == face) {
            Emit(output.coerceIn(0, REDSTONE_MAX_POWER))
        } else {
            Emit.ZERO
        }
}
