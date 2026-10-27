package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-player sculk shrieker warning levels, matching vanilla's 0-4 scale, 200 tick cooldown and
 * one-level decay after 12000 ticks without a new warning. Summoning is intentionally not handled.
 */
internal object WardenSpawnTracker {
    private data class State(
        var level: Int = 0,
        var lastWarningTick: Long = -1,
        var cooldownUntil: Long = -1,
    )

    private val states = ConcurrentHashMap<UUID, State>()

    fun blame(
        player: UUID,
        tick: Long,
    ): Int {
        val state = states.computeIfAbsent(player) { State() }
        decay(state, tick)
        if (tick < state.cooldownUntil) return state.level
        state.level = (state.level + 1).coerceAtMost(MAX_WARNING_LEVEL)
        state.lastWarningTick = tick
        state.cooldownUntil = tick + COOLDOWN_TICKS
        return state.level
    }

    fun warningLevel(
        player: UUID,
        tick: Long,
    ): Int {
        val state = states[player] ?: return 0
        decay(state, tick)
        return state.level
    }

    fun reset(player: UUID) {
        states.remove(player)
    }

    private fun decay(
        state: State,
        tick: Long,
    ) {
        if (state.lastWarningTick < 0 || state.level <= 0) return
        val steps = ((tick - state.lastWarningTick) / DECAY_TICKS).toInt()
        if (steps <= 0) return
        state.level = (state.level - steps).coerceAtLeast(0)
        state.lastWarningTick += steps * DECAY_TICKS
    }

    private const val MAX_WARNING_LEVEL = 4
    private const val COOLDOWN_TICKS = 200L
    private const val DECAY_TICKS = 12000L
}
