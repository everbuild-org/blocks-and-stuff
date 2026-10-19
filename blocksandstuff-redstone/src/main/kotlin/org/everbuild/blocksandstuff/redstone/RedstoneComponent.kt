package org.everbuild.blocksandstuff.redstone

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace

/** Signal a component emits through a single face. */
data class Emit(
    val weak: Int = 0,
    val direct: Int = 0,
) {
    val active: Boolean get() = weak > 0 || direct > 0

    companion object {
        val ZERO: Emit = Emit()
    }
}

/** A world-mutating effect requested by a component (piston movement, TNT, dispenser, ...). */
fun interface SideEffect {
    fun apply(ctx: UpdateContext)
}

/** Result of evaluating a component for one position. */
sealed interface ComponentEvaluation {
    data object None : ComponentEvaluation

    data class SetState(
        val block: Block,
        val scheduleTick: Boolean = false,
    ) : ComponentEvaluation

    data class Trigger(val effect: SideEffect) : ComponentEvaluation
}

/**
 * Behaviour of a single redstone component at a single position.
 *
 * Instances are created per position by the simulator and may hold in-memory state (e.g. the derived
 * analog value of a comparator). They are recreated when the underlying block type changes.
 */
interface RedstoneComponent {
    /** Recompute the desired state from the current inputs. */
    fun evaluate(ctx: UpdateContext): ComponentEvaluation = ComponentEvaluation.None

    /** Signal this component emits through [face]. */
    fun emit(ctx: ReadContext, face: BlockFace): Emit = Emit.ZERO

    /** Faces this component consumes input from. Default: all faces. */
    fun consumes(ctx: ReadContext): Set<BlockFace> = ALL_FACES

    /** Delay in ticks before the result of [evaluate] takes effect. `0` = immediate. */
    fun delayTicks(ctx: ReadContext): Int = 0

    /** Hook for components that react to a specific neighbour change. */
    fun onNeighbourChanged(
        ctx: UpdateContext,
        changed: Point,
        face: BlockFace,
    ) {
        ctx.markDirty(ctx.position)
    }

    /** Hook for delayed components when a scheduled tick fires. */
    fun onScheduledTick(ctx: UpdateContext) {
        ctx.markDirty(ctx.position)
    }
}

/** Read-only context passed to [RedstoneComponent.emit] and related queries. */
interface ReadContext {
    val instance: Instance
    val position: Point

    /** Current simulator tick. */
    fun tick(): Long

    fun block(pos: Point = position): Block

    fun facing(block: Block = block()): BlockFace?

    fun power(): PowerQuery

    fun registry(): RedstoneRegistry

    fun componentAt(pos: Point): RedstoneComponent?
}

/** Mutable context passed to [RedstoneComponent.evaluate]. */
interface UpdateContext : ReadContext {
    fun setState(block: Block) = setState(position, block)

    fun setState(
        pos: Point,
        block: Block,
    )

    fun scheduleTick(delayTicks: Int) = scheduleTick(position, delayTicks)

    fun scheduleTick(
        pos: Point,
        delayTicks: Int,
    )

    fun markDirty(pos: Point = position)
}
