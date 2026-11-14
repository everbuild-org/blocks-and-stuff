package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.EntityType
import net.minestom.server.event.entity.EntitySpawnEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Lightning rod. While `powered`, emits weak power in all directions and strong power into the
 * block it is attached to. A strike powers it for 8 game ticks.
 */
class LightningRodComponent : RedstoneComponent {
    private var resetAt = -1L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        if (block.getProperty("powered") != "true") {
            resetAt = -1L
            return ComponentEvaluation.None
        }

        val now = ctx.tick()
        if (resetAt < 0L) {
            val delay = (ACTIVATION_TICKS - 1).coerceAtLeast(1)
            resetAt = now + delay
            ctx.scheduleTick(delay)
            return ComponentEvaluation.None
        }
        if (now < resetAt) return ComponentEvaluation.None

        resetAt = -1L
        return ComponentEvaluation.SetState(block.withProperty("powered", "false"))
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit {
        val block = ctx.block()
        if (block.getProperty("powered") != "true") return Emit.ZERO
        val facing = Facing.of(block) ?: return Emit.ZERO
        return if (face == facing.oppositeFace) {
            Emit(REDSTONE_MAX_POWER, REDSTONE_MAX_POWER)
        } else {
            Emit(REDSTONE_MAX_POWER, 0)
        }
    }

    private companion object {
        private const val ACTIVATION_TICKS = 8
    }
}

object LightningRodBlockRegistration {
    private var registeredHandler: Any? = null

    @JvmStatic
    fun registerDefault() {
        val handler = MinecraftServer.getGlobalEventHandler()
        if (registeredHandler === handler) return
        registeredHandler = handler

        handler.addListener(EntitySpawnEvent::class.java) { event ->
            if (event.entity.entityType != EntityType.LIGHTNING_BOLT) return@addListener
            val instance = event.instance
            val center = event.entity.position.asBlockVec()
            for (dx in -2..2) {
                for (dy in -2..2) {
                    for (dz in -2..2) {
                        val position = center.add(dx.toDouble(), dy.toDouble(), dz.toDouble())
                        if (isLightningRod(instance.getBlock(position))) strike(instance, position)
                    }
                }
            }
        }
    }

    internal fun strike(
        instance: Instance,
        position: Point,
    ) {
        val block = instance.getBlock(position)
        if (!isLightningRod(block) || block.getProperty("powered") == "true") return
        instance.setBlock(position, block.withProperty("powered", "true"), false)
    }

    private fun isLightningRod(block: Block): Boolean = block.key().value().contains("lightning_rod")
}
