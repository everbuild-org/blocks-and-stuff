package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Target block. Emits weak power equal to `power` in all directions. A projectile hit sets the
 * power and schedules a vanilla reset (20 game ticks for arrows, 8 for other projectiles).
 */
class TargetComponent : RedstoneComponent {
    private var resetAt = -1L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val power = block.getProperty("power")?.toIntOrNull()?.coerceIn(0, REDSTONE_MAX_POWER) ?: 0
        if (power <= 0) {
            resetAt = -1L
            return ComponentEvaluation.None
        }

        val now = ctx.tick()
        if (resetAt < 0L) {
            val ticks = (block.getTag(RESET_TAG) ?: DEFAULT_RESET_TICKS).coerceAtLeast(1)
            val delay = (ticks - 1).coerceAtLeast(1)
            resetAt = now + delay
            ctx.scheduleTick(delay)
            return ComponentEvaluation.None
        }
        if (now < resetAt) return ComponentEvaluation.None

        resetAt = -1L
        return ComponentEvaluation.SetState(block.withProperty("power", "0").withTag(RESET_TAG, null))
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit {
        val power = ctx.block().getProperty("power")?.toIntOrNull()?.coerceIn(0, REDSTONE_MAX_POWER) ?: 0
        return if (power > 0) Emit(power) else Emit.ZERO
    }

    internal companion object {
        internal val RESET_TAG: Tag<Int> = Tag.Integer("blocksandstuff:target_reset_ticks")
        private const val DEFAULT_RESET_TICKS = 8
    }
}

object TargetBlockRegistration {
    private var registeredHandler: Any? = null
    private val arrowTypes = setOf(EntityType.ARROW, EntityType.SPECTRAL_ARROW, EntityType.TRIDENT)

    @JvmStatic
    fun registerDefault() {
        val handler = MinecraftServer.getGlobalEventHandler()
        if (registeredHandler === handler) return
        registeredHandler = handler

        handler.addListener(ProjectileCollideWithBlockEvent::class.java) { event ->
            val instance = event.instance
            val position = event.collisionPosition.asBlockVec()
            val block = instance.getBlock(position)
            if (!block.compare(Block.TARGET)) return@addListener
            if ((block.getProperty("power")?.toIntOrNull() ?: 0) > 0) return@addListener

            val power = targetPower(event.collisionPosition, event.entity.velocity)
            val ticks = if (event.entity.entityType in arrowTypes) 20 else 8
            hit(instance, position, power, ticks)
        }
    }

    internal fun hit(
        instance: Instance,
        position: Point,
        power: Int,
        ticks: Int,
    ) {
        val block = instance.getBlock(position)
        if (!block.compare(Block.TARGET)) return
        if ((block.getProperty("power")?.toIntOrNull() ?: 0) > 0) return
        instance.setBlock(
            position,
            block
                .withProperty("power", power.coerceIn(1, REDSTONE_MAX_POWER).toString())
                .withTag(TargetComponent.RESET_TAG, ticks.coerceAtLeast(1)),
            false,
        )
    }

    internal fun targetPower(
        hit: Pos,
        velocity: Vec,
    ): Int {
        val dx = abs(frac(hit.x()) - 0.5)
        val dy = abs(frac(hit.y()) - 0.5)
        val dz = abs(frac(hit.z()) - 0.5)
        val axis =
            when {
                abs(velocity.x()) >= abs(velocity.y()) && abs(velocity.x()) >= abs(velocity.z()) -> Axis.X
                abs(velocity.y()) >= abs(velocity.z()) -> Axis.Y
                else -> Axis.Z
            }
        val distance =
            when (axis) {
                Axis.X -> max(dy, dz)
                Axis.Y -> max(dx, dz)
                Axis.Z -> max(dx, dy)
            }
        val strength = 15.0 * ((0.5 - distance) / 0.5).coerceIn(0.0, 1.0)
        return max(1, ceil(strength).toInt())
    }

    private fun frac(value: Double): Double = value - floor(value)

    private enum class Axis {
        X,
        Y,
        Z,
    }
}
