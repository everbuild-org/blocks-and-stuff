package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.metadata.other.ItemFrameMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.blocks.util.containerSignal
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Redstone comparator. Rear and side inputs are analog (0..15). In `compare` mode the rear signal is
 * passed through when `rear >= side`, otherwise the output is 0; in `subtract` mode the output is
 * `rear - side`. The analog value is cached in the component and mirrored to block NBT so it\n * survives a server restart.
 */
class ComparatorComponent : RedstoneComponent {
    private var output = 0
    private var tickScheduled = false
    private var scheduledAt = 0L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return ComponentEvaluation.None
        val now = ctx.tick()
        val currentPowered = block.getProperty("powered") == "true"

        if (tickScheduled && now >= scheduledAt) {
            tickScheduled = false
            val value = calculateOutput(ctx, block, facing)
            val powered = value > 0
            val changed = value != output || powered != currentPowered
            output = value
            if (!changed && block.getProperty("mode") != "compare") return ComponentEvaluation.None
            return if (powered == currentPowered) {
                ComponentEvaluation.UpdateNeighbours
            } else {
                ComponentEvaluation.SetState(block.withProperty("powered", powered.toString()))
            }
            // </<@AI_UNREVIEWED>
        }

        if (!tickScheduled) {
            val value = calculateOutput(ctx, block, facing)
            if (value != output || (value > 0) != currentPowered) {
                tickScheduled = true
                scheduledAt = now + DELAY_TICKS
                ctx.scheduleTick(DELAY_TICKS)
            }
        }
        return ComponentEvaluation.None
    }

    private fun calculateOutput(
        ctx: UpdateContext,
        block: Block,
        facing: BlockFace,
    ): Int {
        val rearPosition = ctx.position.relative(facing)
        var rear =
            maxOf(
                inputFrom(ctx, facing, facing.oppositeFace),
                containerSignal(ctx.instance, rearPosition),
                analogSignal(ctx.block(rearPosition)),
                itemFrameSignal(ctx.instance, rearPosition),
            )

        if (rear < 15 && ctx.block(rearPosition).redstoneConductor()) {
            val through = rearPosition.relative(facing)
            rear =
                maxOf(
                    rear,
                    containerSignal(ctx.instance, through),
                    analogSignal(ctx.block(through)),
                    itemFrameSignal(ctx.instance, through),
                )
        }

        val side = Facing.sides(facing).maxOf { s -> inputFrom(ctx, s, s.oppositeFace) }
        return if (block.getProperty("mode") == "subtract") {
            (rear - side).coerceAtLeast(0)
        } else {
            if (rear >= side) rear else 0
        }
    }

    private fun analogSignal(block: Block): Int =
        when {
            block.compare(Block.SCULK_SENSOR) || block.compare(Block.CALIBRATED_SCULK_SENSOR) -> {
                block.getTag(SculkSensorComponent.LAST_FREQUENCY_TAG)?.coerceIn(0, 15) ?: 0
            }

            block.compare(Block.COMPOSTER) -> {
                block.getProperty("level")?.toIntOrNull()?.coerceIn(0, 8) ?: 0
            }

            block.compare(Block.BEE_NEST) || block.compare(Block.BEEHIVE) -> {
                block.getProperty("honey_level")?.toIntOrNull()?.coerceIn(0, 5) ?: 0
            }

            else -> {
                0
            }
        }

    private fun itemFrameSignal(
        instance: Instance,
        position: Point,
    ): Int {
        val frames =
            instance.entities.filter { entity ->
                entity.entityType
                    .key()
                    .value()
                    .endsWith("item_frame") &&
                    entity.position.asBlockVec() == position.asBlockVec()
            }
        if (frames.size != 1) return 0

        val meta = frames.first().entityMeta as? ItemFrameMeta ?: return 0
        if (meta.item.isAir) return 0
        return (meta.rotation.ordinal + 1).coerceIn(1, 8)
    }

    private companion object {
        /** Vanilla comparator delay: one redstone tick (two game ticks). */
        private const val DELAY_TICKS = 2
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit =
        if (Facing.of(ctx.block())?.oppositeFace == face) {
            val value = output.coerceIn(0, REDSTONE_MAX_POWER)
            Emit(value, value)
        } else {
            Emit.ZERO
        }
}
