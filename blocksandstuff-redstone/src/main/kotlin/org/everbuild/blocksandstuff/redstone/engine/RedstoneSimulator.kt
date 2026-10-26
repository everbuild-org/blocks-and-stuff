package org.everbuild.blocksandstuff.redstone.engine

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.PowerQuery
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.RedstoneRegistry
import org.everbuild.blocksandstuff.redstone.UpdateContext
import org.slf4j.LoggerFactory
import java.util.ArrayDeque
import java.util.PriorityQueue

/**
 * Per-instance event-driven redstone simulator.
 *
 * Block changes schedule the affected position and its neighbours for the next tick. Within a tick a
 * delta-cycle FIFO settles combinational logic until quiescence; sequential components re-schedule
 * themselves with their delay. Power is always derived via [weakSignal] / [directSignal], never read
 * back from a plain block.
 */
class RedstoneSimulator(
    private val instance: Instance,
    private val registry: RedstoneRegistry,
) : PowerQuery {
    private data class ScheduledTick(
        val tick: Long,
        val sequence: Long,
        val position: Point,
    )

    private data class Cached(
        val blockKey: Key,
        val component: RedstoneComponent,
    )

    private val logger = LoggerFactory.getLogger(RedstoneSimulator::class.java)
    private val tickQueue = PriorityQueue<ScheduledTick>(compareBy({ it.tick }, { it.sequence }))
    private val components = HashMap<Point, Cached>()
    private val delta = ArrayDeque<Point>()
    private val deltaSet = HashSet<Point>()
    private var currentTick = 0L
    private var sequence = 0L
    private var processing = false

    /** Delta-cycle steps processed by the most recent [tick]. Diagnostic hook for tests/metrics. */
    var lastDeltaSteps: Int = 0
        private set

    override fun weakSignal(
        from: Point,
        face: BlockFace,
    ): Int {
        val position = canonical(from)
        val component =
            componentAt(position)
                ?: // Strongly powered solid blocks conduct their strength as weak power to their neighbours.
                return conductedSignal(position).coerceIn(0, REDSTONE_MAX_POWER)
        return component.emit(ReadContextImpl(position), face).weak.coerceIn(0, REDSTONE_MAX_POWER)
    }

    private fun conductedSignal(position: Point): Int {
        if (!instance.getBlock(position).solid()) return 0
        var best = 0
        for (face in BlockFace.entries) {
            val neighbour = position.relative(face)
            val signal = directSignal(neighbour, face.oppositeFace)
            if (signal > best) {
                best = signal
                if (best >= REDSTONE_MAX_POWER) break
            }
        }
        return best
    }

    override fun directSignal(
        from: Point,
        face: BlockFace,
    ): Int {
        val position = canonical(from)
        val component = componentAt(position) ?: return 0
        return component.emit(ReadContextImpl(position), face).direct.coerceIn(0, REDSTONE_MAX_POWER)
    }

    /** Called when a block in the instance changed; schedules re-evaluation for the next tick. */
    fun onBlockChanged(position: Point) {
        val pos = canonical(position)
        invalidate(pos)
        schedule(pos, 1)
        for (face in BlockFace.entries) {
            val neighbour = pos.relative(face)
            schedule(neighbour, 1)

            val block = instance.getBlock(neighbour)
            if (!block.air() && componentAt(neighbour) == null) {
                for (secondFace in BlockFace.entries) schedule(neighbour.relative(secondFace), 1)
            }
        }
    }

    /** Processes all updates due for the current server tick. */
    fun tick() {
        currentTick++

        delta.clear()
        deltaSet.clear()
        while (tickQueue.isNotEmpty() && tickQueue.peek().tick <= currentTick) {
            val next = tickQueue.poll()
            if (deltaSet.add(next.position)) delta.add(next.position)
        }
        if (delta.isEmpty()) {
            lastDeltaSteps = 0
            return
        }

        processing = true
        try {
            var guard = 0
            var steps = 0
            while (delta.isNotEmpty()) {
                if (guard++ >= DELTA_LIMIT) {
                    logger.warn("Redstone delta cycle did not settle after {} steps in {}", DELTA_LIMIT, instance.uuid)
                    break
                }
                steps++
                val position = delta.removeFirst()
                deltaSet.remove(position)
                evaluate(position)
            }
            lastDeltaSteps = steps
        } finally {
            processing = false
        }
    }

    private fun evaluate(position: Point) {
        val block = instance.getBlock(position)
        val component = componentAt(position) ?: return
        val ctx = UpdateContextImpl(position)

        when (val evaluation = component.evaluate(ctx)) {
            is ComponentEvaluation.None -> {
                Unit
            }

            is ComponentEvaluation.SetState -> {
                if (!sameState(evaluation.block, block)) {
                    instance.setBlock(position, evaluation.block, false)
                    invalidate(position)
                }
                enqueueNeighbours(position)
                if (evaluation.scheduleTick) {
                    schedule(position, component.delayTicks(ctx))
                }
            }

            is ComponentEvaluation.Trigger -> {
                evaluation.effect.apply(ctx)
                enqueueNeighbours(position)
            }
        }
    }

    private fun componentAt(position: Point): RedstoneComponent? {
        val pos = canonical(position)
        val block = instance.getBlock(pos)
        val cached = components[pos]
        if (cached != null && cached.blockKey == block.key()) return cached.component

        val factory = registry.factoryFor(block)
        if (factory == null) {
            components.remove(pos)
            return null
        }

        val component = factory(block)
        components[pos] = Cached(block.key(), component)
        return component
    }

    private fun sameState(
        a: Block,
        b: Block,
    ): Boolean = a.stateId() == b.stateId() && a.nbt() == b.nbt()

    private fun invalidate(position: Point) {
        val pos = canonical(position)
        val cached = components[pos] ?: return
        if (cached.blockKey != instance.getBlock(pos).key()) components.remove(pos)
    }

    /** Canonical block coordinates so `Pos` and `Vec` keys for the same block are treated equally. */
    private fun canonical(position: Point): Point = position.asBlockVec()

    private fun schedule(
        position: Point,
        delayTicks: Int,
    ) {
        val pos = canonical(position)
        tickQueue.add(ScheduledTick(currentTick + delayTicks.coerceAtLeast(1), sequence++, pos))
    }

    private fun enqueue(position: Point) {
        val pos = canonical(position)
        if (!processing) {
            schedule(pos, 1)
            return
        }
        if (deltaSet.add(pos)) delta.add(pos)
    }

    private fun enqueueNeighbours(position: Point) {
        for (face in BlockFace.entries) {
            val neighbour = position.relative(face)
            enqueue(neighbour)

            val block = instance.getBlock(neighbour)
            if (!block.air() && componentAt(neighbour) == null) {
                for (secondFace in BlockFace.entries) enqueue(neighbour.relative(secondFace))
            }
        }
    }

    private open inner class ReadContextImpl(
        override val position: Point,
    ) : ReadContext {
        override val instance: Instance get() = this@RedstoneSimulator.instance

        override fun tick(): Long = this@RedstoneSimulator.currentTick

        override fun block(pos: Point): Block = instance.getBlock(pos)

        override fun facing(block: Block): BlockFace? =
            when (block.getProperty("facing")) {
                "north" -> BlockFace.NORTH
                "south" -> BlockFace.SOUTH
                "east" -> BlockFace.EAST
                "west" -> BlockFace.WEST
                "up" -> BlockFace.TOP
                "down" -> BlockFace.BOTTOM
                else -> null
            }

        override fun power(): PowerQuery = this@RedstoneSimulator

        override fun registry(): RedstoneRegistry = this@RedstoneSimulator.registry

        override fun componentAt(pos: Point): RedstoneComponent? = this@RedstoneSimulator.componentAt(pos)
    }

    private inner class UpdateContextImpl(
        position: Point,
    ) : ReadContextImpl(position),
        UpdateContext {
        override fun setState(
            pos: Point,
            block: Block,
        ) {
            if (sameState(instance.getBlock(pos), block)) return
            instance.setBlock(pos, block, false)
            invalidate(pos)
            enqueue(pos)
            enqueueNeighbours(pos)
        }

        override fun scheduleTick(
            pos: Point,
            delayTicks: Int,
        ) {
            schedule(pos, delayTicks)
        }

        override fun markDirty(pos: Point) {
            enqueue(pos)
        }
    }

    private companion object {
        private const val DELTA_LIMIT = 4096
    }
}
