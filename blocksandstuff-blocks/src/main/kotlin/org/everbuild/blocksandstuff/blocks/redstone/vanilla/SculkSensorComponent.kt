package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import org.everbuild.blocksandstuff.redstone.Vibration
import org.everbuild.blocksandstuff.redstone.VibrationBus
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Sculk sensor. Mimics the vanilla listening cycle: detection radius, one-tick-per-block travel
 * time, active phase, cooldown, frequency filtering for calibrated sensors, wool occlusion,
 * amethyst resonance and waterlogged silence.
 */
class SculkSensorComponent(
    private val calibrated: Boolean,
) : RedstoneComponent {
    private var initialized = false
    private var active = false
    private var activeUntil = -1L
    private var cooldownUntil = -1L
    private val detectionRadius = if (calibrated) 16 else 8
    private val activeTicks = if (calibrated) 10 else 30

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        VibrationBus.registerSensor(ctx.instance, ctx.position, detectionRadius)

        val block = ctx.block()
        val now = ctx.tick()

        if (!initialized) {
            initialized = true
            cooldownUntil = now + COOLDOWN_TICKS
            ctx.scheduleTick(COOLDOWN_TICKS)
            return setState(block, 0, "cooldown")
        }

        if (active) {
            if (now < activeUntil) return ComponentEvaluation.None
            active = false
            playClicking(ctx, block, start = false)
            cooldownUntil = now + COOLDOWN_TICKS
            ctx.scheduleTick(COOLDOWN_TICKS)
            return setState(block, 0, "cooldown")
        }

        if (cooldownUntil >= 0) {
            if (now < cooldownUntil) {
                VibrationBus.consume(ctx.instance, ctx.position)
                return ComponentEvaluation.None
            }
            cooldownUntil = -1
            return setState(block, 0, "inactive")
        }

        val vibration = VibrationBus.consume(ctx.instance, ctx.position) ?: return ComponentEvaluation.None
        if (!matches(ctx, block, vibration)) return ComponentEvaluation.None

        active = true
        activeUntil = now + activeTicks
        ctx.scheduleTick(activeTicks)
        playClicking(ctx, block, start = true)
        spawnVibrationParticle(ctx, vibration)
        resonateAmethyst(ctx, vibration)

        val updated =
            setState(block, strengthFor(ctx, vibration), "active")
                .let { evaluation ->
                    (evaluation as ComponentEvaluation.SetState).block.withTag(LAST_FREQUENCY_TAG, vibration.frequency)
                }
        return ComponentEvaluation.SetState(updated)
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit {
        if (calibrated && Facing.of(ctx.block()) == face) return Emit.ZERO
        val power = ctx.block().getProperty("power")?.toIntOrNull()?.coerceIn(0, REDSTONE_MAX_POWER) ?: 0
        if (power <= 0) return Emit.ZERO
        return Emit(power, if (face == BlockFace.BOTTOM) power else 0)
    }

    private fun matches(
        ctx: UpdateContext,
        block: Block,
        vibration: Vibration,
    ): Boolean {
        if (!calibrated) return true
        val tuned = tunedFrequency(ctx, block)
        return tuned == 0 || tuned == vibration.frequency
    }

    private fun tunedFrequency(
        ctx: UpdateContext,
        block: Block,
    ): Int {
        val facing = Facing.of(block) ?: return 0
        val neighbour = ctx.position.relative(facing)
        val into = facing.oppositeFace
        return maxOf(
            ctx.power().weakSignal(neighbour, into),
            ctx.power().directSignal(neighbour, into),
        ).coerceIn(0, REDSTONE_MAX_POWER)
    }

    private fun strengthFor(
        ctx: UpdateContext,
        vibration: Vibration,
    ): Int {
        val dx = ctx.position.blockX() - vibration.origin.blockX()
        val dy = ctx.position.blockY() - vibration.origin.blockY()
        val dz = ctx.position.blockZ() - vibration.origin.blockZ()
        val distance = sqrt((dx * dx + dy * dy + dz * dz).toDouble())
        val strength = REDSTONE_MAX_POWER - floor(REDSTONE_MAX_POWER.toDouble() / detectionRadius * distance).toInt()
        return strength.coerceIn(1, REDSTONE_MAX_POWER)
    }

    private fun playClicking(
        ctx: UpdateContext,
        block: Block,
        start: Boolean,
    ) {
        if (isWaterlogged(block)) return
        val event = if (start) SoundEvent.BLOCK_SCULK_SENSOR_CLICKING else SoundEvent.BLOCK_SCULK_SENSOR_CLICKING_STOP
        val volume = if (start) 0.73f else 0.62f
        val pitch = Random.nextDouble(0.8, 1.0).toFloat()
        ctx.instance.playSoundExcept(
            null,
            Sound.sound(event, Sound.Source.BLOCK, volume, pitch),
            ctx.position,
        )
    }

    private fun spawnVibrationParticle(
        ctx: UpdateContext,
        vibration: Vibration,
    ) {
        val particle = Particle.VIBRATION as Particle.Vibration
        ctx.instance.sendGroupedPacket(
            ParticlePacket(
                particle.withSourceBlockPosition(vibration.origin, vibration.travelTicks),
                ctx.position.add(0.5, 0.5, 0.5),
                Vec.ZERO,
                0.0f,
                1,
            ),
        )
    }

    private fun resonateAmethyst(
        ctx: UpdateContext,
        vibration: Vibration,
    ) {
        for (face in BlockFace.entries) {
            val position = ctx.position.relative(face)
            if (!ctx.block(position).compare(Block.AMETHYST_BLOCK)) continue
            VibrationBus.emit(ctx.instance, position, vibration.frequency)
            val pitch = AMETHYST_PITCHES[vibration.frequency.coerceIn(0, 15)]
            val pitchValue = 2.0.pow((pitch - 12) / 12.0).toFloat()
            ctx.instance.playSoundExcept(
                null,
                Sound.sound(SoundEvent.BLOCK_AMETHYST_BLOCK_RESONATE, Sound.Source.BLOCK, 1.0f, pitchValue),
                position,
            )
        }
    }

    private fun isWaterlogged(block: Block): Boolean = block.getProperty("waterlogged") == "true"

    private fun setState(
        block: Block,
        power: Int,
        phase: String,
    ): ComponentEvaluation {
        var updated = block.withProperty("power", power.toString())
        if (updated.properties().containsKey("sculk_sensor_phase")) {
            updated = updated.withProperty("sculk_sensor_phase", phase)
        }
        return ComponentEvaluation.SetState(updated)
    }

    internal companion object {
        private const val REDSTONE_MAX_POWER = 15
        private const val COOLDOWN_TICKS = 10
        private val AMETHYST_PITCHES = intArrayOf(0, 0, 2, 4, 6, 7, 9, 10, 12, 14, 15, 18, 19, 21, 22, 24)

        internal val LAST_FREQUENCY_TAG: Tag<Int> = Tag.Integer("blocksandstuff:last_vibration_frequency")
    }
}
