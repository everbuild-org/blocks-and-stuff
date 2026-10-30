package org.everbuild.blocksandstuff.redstone

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace

/** Generic component backing [RedstoneRegistry.source]. */
internal class SourceComponent(
    private val concrete: Block,
    private val source: PowerSource,
) : RedstoneComponent {
    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit {
        val strength = source.strength(ctx.block()).coerceIn(0, REDSTONE_MAX_POWER)
        if (strength <= 0) return Emit.ZERO
        if (source.directional(concrete) && ctx.facing(ctx.block()) != face) return Emit.ZERO
        return if (source.direct(ctx.block())) Emit(strength, strength) else Emit(strength, 0)
    }
}

/** Generic component backing [RedstoneRegistry.consumer]. */
internal class ConsumerComponent(
    private val consumer: PowerConsumer,
) : RedstoneComponent {
    private var powered = false

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val strength = ctx.power().signalInto(ctx.position)
        val now = strength > 0
        if (now == powered) return ComponentEvaluation.None

        powered = now
        val replacement = consumer.onPoweredChanged(ctx, now, strength)
        return if (replacement != null) ComponentEvaluation.SetState(replacement) else ComponentEvaluation.None
    }
}
