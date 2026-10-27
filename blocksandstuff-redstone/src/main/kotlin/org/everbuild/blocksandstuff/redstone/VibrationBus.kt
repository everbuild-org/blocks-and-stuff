package org.everbuild.blocksandstuff.redstone

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.Instance
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/** A vibration emitted in the world, identified by its frequency (1-15). */
data class Vibration(
    val origin: Point,
    val frequency: Int,
    val travelTicks: Int,
)

/**
 * Per-instance vibration bus for sculk sensors.
 *
 * Sensors register their position and detection radius while being evaluated. [emit] stores the
 * vibration for nearby sensors and schedules them after the vanilla one-block-per-tick travel time.
 */
object VibrationBus {
    private val sensors = ConcurrentHashMap<Instance, ConcurrentHashMap<Point, Int>>()
    private val pending = ConcurrentHashMap<Instance, ConcurrentHashMap<Point, Vibration>>()

    fun registerSensor(
        instance: Instance,
        position: Point,
        radius: Int,
    ) {
        sensors.getOrPut(instance) { ConcurrentHashMap() }[position.asBlockVec()] = radius
    }

    fun emit(
        instance: Instance,
        origin: Point,
        frequency: Int,
    ) {
        val installedSensors = sensors[instance] ?: return
        val originVec = origin.asBlockVec()
        for ((position, radius) in installedSensors) {
            val dx = position.blockX() - originVec.blockX()
            val dy = position.blockY() - originVec.blockY()
            val dz = position.blockZ() - originVec.blockZ()
            val distance = ceil(sqrt((dx * dx + dy * dy + dz * dz).toDouble())).toInt()
            if (distance > radius) continue
            if (isOccluded(instance, originVec, position)) continue

            val travelTicks = distance.coerceAtLeast(1)
            pending.getOrPut(instance) { ConcurrentHashMap() }.putIfAbsent(position, Vibration(originVec, frequency, travelTicks))
            RedstoneRegistrations.notifyBlockChanged(instance, position, travelTicks)
        }
    }

    private fun isOccluded(
        instance: Instance,
        origin: Point,
        target: Point,
    ): Boolean {
        var x = origin.blockX()
        var y = origin.blockY()
        var z = origin.blockZ()
        val endX = target.blockX()
        val endY = target.blockY()
        val endZ = target.blockZ()
        if (x == endX && y == endY && z == endZ) return false

        val deltaX = endX - x
        val deltaY = endY - y
        val deltaZ = endZ - z
        val stepX = deltaX.compareTo(0)
        val stepY = deltaY.compareTo(0)
        val stepZ = deltaZ.compareTo(0)

        val tDeltaX = if (deltaX == 0) Double.POSITIVE_INFINITY else 1.0 / abs(deltaX)
        val tDeltaY = if (deltaY == 0) Double.POSITIVE_INFINITY else 1.0 / abs(deltaY)
        val tDeltaZ = if (deltaZ == 0) Double.POSITIVE_INFINITY else 1.0 / abs(deltaZ)
        var tMaxX = if (deltaX == 0) Double.POSITIVE_INFINITY else 0.5 / abs(deltaX)
        var tMaxY = if (deltaY == 0) Double.POSITIVE_INFINITY else 0.5 / abs(deltaY)
        var tMaxZ = if (deltaZ == 0) Double.POSITIVE_INFINITY else 0.5 / abs(deltaZ)

        while (x != endX || y != endY || z != endZ) {
            when {
                tMaxX < tMaxY && tMaxX < tMaxZ -> {
                    x += stepX
                    tMaxX += tDeltaX
                }

                tMaxY < tMaxZ -> {
                    y += stepY
                    tMaxY += tDeltaY
                }

                else -> {
                    z += stepZ
                    tMaxZ += tDeltaZ
                }
            }
            if (x == endX && y == endY && z == endZ) break
            val key = instance.getBlock(Vec(x.toDouble(), y.toDouble(), z.toDouble())).key().value()
            if (key.contains("wool") || key.contains("carpet")) return true
        }
        return false
    }

    fun consume(
        instance: Instance,
        position: Point,
    ): Vibration? = pending[instance]?.remove(position.asBlockVec())

    fun clear(instance: Instance) {
        sensors.remove(instance)
        pending.remove(instance)
    }
}
