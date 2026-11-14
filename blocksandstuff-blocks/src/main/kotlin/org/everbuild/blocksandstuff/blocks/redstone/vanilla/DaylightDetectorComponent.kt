package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Daylight detector. Emits weak power based on sky light and the default clock time, re-evaluated
 * once per second like vanilla. `inverted` turns it into `15 - (skyLight - skyDarken)`.
 */
class DaylightDetectorComponent : RedstoneComponent {
    private var nextUpdate = -1L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        if (ctx.tick() >= nextUpdate) {
            ctx.scheduleTick(UPDATE_INTERVAL_TICKS)
            nextUpdate = ctx.tick() + UPDATE_INTERVAL_TICKS
        }

        val block = ctx.block()
        val updated = block.withProperty("power", powerAt(ctx).toString())
        return if (updated.stateId() == block.stateId() && updated.nbt() == block.nbt()) {
            ComponentEvaluation.None
        } else {
            ComponentEvaluation.SetState(updated)
        }
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit =
        Emit(
            ctx
                .block()
                .getProperty("power")
                ?.toIntOrNull()
                ?.coerceIn(0, REDSTONE_MAX_POWER) ?: 0,
        )

    private fun powerAt(ctx: ReadContext): Int {
        val dimension =
            MinecraftServer.getRegistries().dimensionType().get(ctx.instance.getDimensionType()) ?: return 0
        val skyLight = if (dimension.hasSkylight()) skyLightAbove(ctx, dimension.maxY()) else 0
        val skyDarken = if (dimension.hasSkylight()) skyDarken(ctx) else 0

        var power = skyLight - skyDarken
        if (ctx.block().getProperty("inverted") == "true") {
            power = REDSTONE_MAX_POWER - power
        } else if (power > 0) {
            var angle = sunAngle(ctx)
            val target = if (angle < PI.toFloat()) 0.0f else (2 * PI).toFloat()
            angle += (target - angle) * 0.2f
            power = (power * cos(angle)).roundToInt()
        }
        return power.coerceIn(0, REDSTONE_MAX_POWER)
    }

    /**
     * Samples the block above instead of the detector itself: Minestom's light engine dims the
     * detector's own cell like any other block, while vanilla reports the unobstructed sky light
     * for it.
     */
    private fun skyLightAbove(
        ctx: ReadContext,
        maxY: Int,
    ): Int {
        val x = ctx.position.blockX()
        val y = ctx.position.blockY()
        val z = ctx.position.blockZ()
        return if (y + 1 < maxY) ctx.instance.getSkyLight(x, y + 1, z) else ctx.instance.getSkyLight(x, y, z)
    }

    private fun skyDarken(ctx: ReadContext): Int {
        val weather = ctx.instance.getWeather()
        val rain = weather.rainLevel.coerceIn(0.0f, 1.0f).toDouble()
        val thunder = (weather.thunderLevel * weather.rainLevel).coerceIn(0.0f, 1.0f).toDouble()
        val rainFactor = 1.0 - rain * 5.0 / 16.0
        val thunderFactor = 1.0 - thunder * 5.0 / 16.0
        val daylight = 0.5 + 2.0 * cos(sunAngle(ctx).toDouble()).coerceIn(-0.25, 0.25)
        return ((1.0 - daylight * rainFactor * thunderFactor) * 11.0).toInt().coerceIn(0, 11)
    }

    private fun sunAngle(ctx: ReadContext): Float = timeOfDay(ctx.instance.getTime()) * (2 * PI).toFloat()

    private fun timeOfDay(dayTime: Long): Float {
        val fraction = frac(dayTime / 24000.0 - 0.25)
        val brightness = 0.5 - cos(fraction * PI) / 2.0
        return ((fraction * 2.0 + brightness) / 3.0).toFloat()
    }

    private fun frac(value: Double): Double = value - floor(value)

    private companion object {
        private const val UPDATE_INTERVAL_TICKS = 20
    }
}
