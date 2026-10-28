package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.blocksandstuff.redstone.UpdateContext
import org.everbuild.blocksandstuff.redstone.VibrationBus
import java.util.concurrent.ThreadLocalRandom

/**
 * Piston and sticky piston, following vanilla movement, push/pull order, quasi-connectivity and
 * two-game-tick moving-piston animation. Block dropping on a cancelled extension is reproduced by
 * immediately finalizing the first pushed block and skipping the sticky pull.
 */
class PistonComponent(
    private val sticky: Boolean,
) : RedstoneComponent {
    private var extending: ExtensionState? = null
    private var pendingBase: PendingBase? = null
    private var lastExtendAttempt = Long.MIN_VALUE

    private data class ExtensionState(
        val firstDestination: Point?,
        val firstBlock: Block?,
        val completeAt: Long,
    )

    private data class PendingBase(
        val block: Block,
        val completeAt: Long,
    )

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val facing = Facing.of(ctx.block()) ?: return ComponentEvaluation.None
        val now = ctx.tick()
        val state = extending
        if (state != null && now >= state.completeAt) extending = null
        val pending = pendingBase
        if (pending != null && now >= pending.completeAt) {
            pendingBase = null
            ctx.setState(pending.block)
        }

        val powered = isPowered(ctx, facing)
        val extended = ctx.block().getProperty("extended") == "true"

        // An extended piston without its head was broken/replaced: remove the orphaned base.
        if (extended && pendingBase == null && !isPistonHead(ctx.block(ctx.position.relative(facing)))) {
            ctx.instance.setBlock(ctx.position, Block.AIR, false)
            return ComponentEvaluation.None
        }

        if (powered && !extended && pendingBase == null && now != lastExtendAttempt) {
            lastExtendAttempt = now
            return ComponentEvaluation.Trigger(facing) { extend(it) }
        }
        if (!powered && extended && pendingBase == null) {
            return ComponentEvaluation.Trigger { retract(it) }
        }
        return ComponentEvaluation.None
    }

    private fun extend(ctx: UpdateContext) {
        val block = ctx.block()
        if (block.getProperty("extended") == "true") return
        val facing = Facing.of(block) ?: return
        val resolver = PistonStructureResolver(ctx.instance, ctx.position, facing, true)
        if (!resolver.resolve()) return

        val now = ctx.tick()
        VibrationBus.emit(ctx.instance, ctx.position, 10)
        ctx.instance.sendBlockAction(ctx.position, EXTEND_ACTION.toByte(), facing.dataValue().toByte())

        for (position in resolver.toDestroy) {
            val destroyed = ctx.instance.getBlock(position)
            DroppedItemFactory.maybeDrop(ctx.instance, position, destroyed)
            ctx.instance.setBlock(position, Block.AIR, false)
        }

        val pushed = resolver.toPush.toList()
        val firstOrigin = pushed.firstOrNull()
        val firstBlock = firstOrigin?.let { ctx.instance.getBlock(it) }
        val firstDestination = firstOrigin?.relative(facing)

        val moves = pushed.map { origin -> origin to ctx.instance.getBlock(origin) }
        for ((origin, _) in moves) {
            setBlockSilently(ctx.instance, origin, Block.AIR)
            RedstoneRegistrations.notifyBlockChanged(ctx.instance, origin)
        }
        for ((origin, moved) in moves.asReversed()) {
            placeMovingBlock(ctx, origin.relative(facing), moved, facing, extending = true, source = false)
        }

        val headPosition = ctx.position.relative(facing)
        placeMovingBlock(ctx, headPosition, head(facing), facing, extending = true, source = true)
        ctx.setState(ctx.position, block.withProperty("extended", "true"))
        extending = ExtensionState(firstDestination, firstBlock, now + MOVEMENT_TICKS)
        ctx.scheduleTick(MOVEMENT_TICKS)
        playSound(ctx, SoundEvent.BLOCK_PISTON_EXTEND)
    }

    private fun placeMovingBlock(
        ctx: UpdateContext,
        position: Point,
        moved: Block,
        facing: BlockFace,
        extending: Boolean,
        source: Boolean,
    ) {
        setBlockSilently(
            ctx.instance,
            position,
            movingPistonBlock(moved, facing, extending = extending, source = source, sticky = sticky),
        )
        RedstoneRegistrations.notifyBlockChanged(ctx.instance, position)
    }

    private fun retract(ctx: UpdateContext) {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return
        val now = ctx.tick()
        val extension = extending
        extending = null

        if (extension != null && now < extension.completeAt) {
            val firstDestination = extension.firstDestination
            val firstBlock = extension.firstBlock
            if (firstDestination != null && firstBlock != null) {
                // Vanilla block dropping: the first pushed block reaches its final position at once.
                ctx.instance.sendBlockAction(ctx.position, DROP_ACTION.toByte(), facing.dataValue().toByte())
                ctx.instance.setBlock(firstDestination, firstBlock, false)
                ctx.instance.setBlock(ctx.position.relative(facing), Block.AIR, false)
                scheduleBaseRetraction(ctx, block, now)
                playSound(ctx, SoundEvent.BLOCK_PISTON_CONTRACT)
                return
            }
        }

        VibrationBus.emit(ctx.instance, ctx.position, 9)
        ctx.instance.sendBlockAction(ctx.position, RETRACT_ACTION.toByte(), facing.dataValue().toByte())
        val headPosition = ctx.position.relative(facing)
        ctx.instance.setBlock(headPosition, Block.AIR, false)

        val pulled =
            if (sticky) {
                val resolver = PistonStructureResolver(ctx.instance, ctx.position, facing, false)
                if (resolver.resolve()) resolver.toPush.toList() else emptyList()
            } else {
                emptyList()
            }

        // Capture all pulled blocks before clearing any origin: origins and destinations can overlap.
        val moves = pulled.map { origin -> origin to ctx.instance.getBlock(origin) }
        for ((origin, _) in moves) {
            setBlockSilently(ctx.instance, origin, Block.AIR)
            RedstoneRegistrations.notifyBlockChanged(ctx.instance, origin)
        }
        for ((origin, moved) in moves.asReversed()) {
            placeMovingBlock(ctx, origin.relative(facing.oppositeFace), moved, facing, extending = false, source = false)
        }

        scheduleBaseRetraction(ctx, block, now)
        playSound(ctx, SoundEvent.BLOCK_PISTON_CONTRACT)
    }

    private fun scheduleBaseRetraction(
        ctx: UpdateContext,
        block: Block,
        now: Long,
    ) {
        pendingBase = PendingBase(block.withProperty("extended", "false"), now + MOVEMENT_TICKS)
        ctx.scheduleTick(MOVEMENT_TICKS)
    }

    private fun head(facing: BlockFace): Block =
        Block.PISTON_HEAD
            .withProperty("facing", facing.propertyValue())
            .withProperty("type", if (sticky) "sticky" else "normal")

    private fun isPistonHead(block: Block): Boolean =
        block.compare(Block.PISTON_HEAD) ||
            (block.compare(Block.MOVING_PISTON) && movedState(block)?.compare(Block.PISTON_HEAD) == true)

    private fun playSound(
        ctx: UpdateContext,
        event: SoundEvent,
    ) {
        ctx.instance.playSoundExcept(
            null,
            Sound.sound(event, Sound.Source.BLOCK, 0.5f, 0.6f + ThreadLocalRandom.current().nextFloat() * 0.25f),
            ctx.position,
        )
    }

    private fun isPowered(
        ctx: ReadContext,
        facing: BlockFace,
    ): Boolean {
        for (face in BlockFace.entries) {
            // Upward-facing pistons can be powered from the head direction by quasi-connectivity.
            if (face == facing && facing != BlockFace.TOP) continue
            if (signalInto(ctx, ctx.position.relative(face), face.oppositeFace) > 0) return true
        }

        val above = ctx.position.add(0.0, 1.0, 0.0)
        for (face in BlockFace.entries) {
            if (face == BlockFace.BOTTOM) continue
            if (signalInto(ctx, above.relative(face), face.oppositeFace) > 0) return true
        }
        return false
    }

    private fun signalInto(
        ctx: ReadContext,
        neighbour: Point,
        into: BlockFace,
    ): Int {
        val block = ctx.block(neighbour)
        if (WireComponent.isWire(block)) return WireComponent.powerOf(block)
        if (ctx.componentAt(neighbour) == null && block.redstoneConductor()) {
            return ctx.power().signalInto(neighbour)
        }
        return maxOf(ctx.power().weakSignal(neighbour, into), ctx.power().directSignal(neighbour, into))
    }

    private companion object {
        private const val MOVEMENT_TICKS = 2
        private const val EXTEND_ACTION = 0
        private const val RETRACT_ACTION = 1
        private const val DROP_ACTION = 2
    }
}
