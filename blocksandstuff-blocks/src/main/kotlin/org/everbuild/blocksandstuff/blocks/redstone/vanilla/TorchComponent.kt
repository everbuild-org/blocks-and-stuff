package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ALL_FACES
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import java.util.ArrayDeque

/**
 * Redstone torch (floor and wall). Lit unless the block it is mounted on is powered by another
 * source; the torch's own contribution to that block is ignored.
 *
 * A torch that is switched off too often within a short window burns out and stays off for a while,
 * like vanilla.
 */
class TorchComponent : RedstoneComponent {
    private var tickScheduled = false
    private var scheduledAt = 0L
    private var burnedOutUntil = -1L
    private val recentOffToggles = ArrayDeque<Long>()

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val support = supportOf(block, ctx.position) ?: return ComponentEvaluation.None
        val now = ctx.tick()
        val currentLit = block.getProperty("lit") == "true"

        if (burnedOutUntil >= 0) {
            if (now < burnedOutUntil) {
                tickScheduled = false
                return if (currentLit) {
                    ComponentEvaluation.SetState(block.withProperty("lit", "false"))
                } else {
                    ComponentEvaluation.None
                }
            }
            burnedOutUntil = -1L
            recentOffToggles.clear()
        }

        if (tickScheduled && now >= scheduledAt) {
            tickScheduled = false
            val lit = !isPowered(ctx, support, ctx.position)
            if (lit == currentLit) return ComponentEvaluation.None
            if (!lit && recordOffToggle(now)) {
                ctx.scheduleTick(BURNOUT_TICKS)
                return ComponentEvaluation.SetState(block.withProperty("lit", "false"))
            }
            return ComponentEvaluation.SetState(block.withProperty("lit", lit.toString()))
        }

        if (!tickScheduled && currentLit == isPowered(ctx, support, ctx.position)) {
            tickScheduled = true
            scheduledAt = now + DELAY_TICKS
            ctx.scheduleTick(DELAY_TICKS)
        }
        return ComponentEvaluation.None
    }

    /**
     * Records an off transition and reports whether the torch has toggled too frequently. When that
     * happens the caller starts the burnout period.
     */
    private fun recordOffToggle(now: Long): Boolean {
        while (recentOffToggles.isNotEmpty() && recentOffToggles.first() < now - BURNOUT_WINDOW_TICKS) {
            recentOffToggles.removeFirst()
        }
        recentOffToggles.addLast(now)
        if (recentOffToggles.size < BURNOUT_TOGGLE_LIMIT) return false

        burnedOutUntil = now + BURNOUT_TICKS
        recentOffToggles.clear()
        return true
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit {
        val block = ctx.block()
        if (block.getProperty("lit") != "true") return Emit.ZERO
        val support = supportOf(block, ctx.position)
        if (support != null && ctx.position.relative(face) == support) return Emit.ZERO
        return if (face == BlockFace.TOP) {
            Emit(REDSTONE_MAX_POWER, REDSTONE_MAX_POWER)
        } else {
            Emit(REDSTONE_MAX_POWER, 0)
        }
    }

    private fun isPowered(
        ctx: ReadContext,
        position: Point,
        exclude: Point,
    ): Boolean {
        for (face in ALL_FACES) {
            val neighbour = position.relative(face)
            if (neighbour == exclude) continue
            val block = ctx.block(neighbour)
            val into = face.oppositeFace
            val signal =
                if (WireComponent.isWire(block)) {
                    ctx.power().weakSignal(neighbour, into)
                } else {
                    maxOf(ctx.power().weakSignal(neighbour, into), ctx.power().directSignal(neighbour, into))
                }
            if (signal > 0) return true
        }
        return false
    }

    companion object {
        /** Vanilla redstone torch delay: one redstone tick (two game ticks). */
        private const val DELAY_TICKS = 2

        private const val BURNOUT_WINDOW_TICKS = 60L
        private const val BURNOUT_TOGGLE_LIMIT = 8
        private const val BURNOUT_TICKS = 160

        fun supportOf(
            block: Block,
            position: Point,
        ): Point? =
            when {
                block.compare(Block.REDSTONE_WALL_TORCH) -> {
                    val facing =
                        when (block.getProperty("facing")) {
                            "north" -> BlockFace.NORTH
                            "south" -> BlockFace.SOUTH
                            "east" -> BlockFace.EAST
                            "west" -> BlockFace.WEST
                            else -> null
                        }
                    facing?.let { position.relative(it.oppositeFace) }
                }

                block.compare(Block.REDSTONE_TORCH) -> {
                    position.relative(BlockFace.BOTTOM)
                }

                else -> {
                    null
                }
            }
    }
}
