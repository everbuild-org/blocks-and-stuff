package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.event.WardenBlameEvent
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import org.everbuild.blocksandstuff.redstone.VibrationBus

/** Sculk shrieker: shrieks on nearby vibrations and applies Darkness afterwards. */
class SculkShriekerComponent : RedstoneComponent {
    private var active = false
    private var shriekingUntil = -1L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        VibrationBus.registerSensor(ctx.instance, ctx.position, DETECTION_RADIUS)

        val block = ctx.block()
        val now = ctx.tick()

        if (active) {
            if (now < shriekingUntil) return ComponentEvaluation.None
            active = false
            applyDarkness(ctx)
            return setShrieking(block, false)
        }

        if (VibrationBus.consume(ctx.instance, ctx.position) == null) return ComponentEvaluation.None
        if (!hasEligiblePlayer(ctx, now)) return ComponentEvaluation.None

        active = true
        shriekingUntil = now + SHRIEK_TICKS
        blameNearbyPlayers(ctx, now)
        ctx.scheduleTick(SHRIEK_TICKS)
        ctx.instance.sendBlockAction(ctx.position, SHRIEK_ACTION, 0)
        playShriek(ctx)
        spawnShriekParticle(ctx)
        return setShrieking(block, true)
    }

    private fun setShrieking(
        block: Block,
        shrieking: Boolean,
    ): ComponentEvaluation {
        if (block.getProperty("shrieking") == null) return ComponentEvaluation.None
        val updated = block.withProperty("shrieking", shrieking.toString())
        return if (updated.stateId() == block.stateId()) ComponentEvaluation.None else ComponentEvaluation.SetState(updated)
    }

    private fun hasEligiblePlayer(
        ctx: UpdateContext,
        now: Long,
    ): Boolean {
        var sawPlayer = false
        for (player in ctx.instance.players) {
            if (player.position.distance(ctx.position.add(0.5, 0.5, 0.5)) > BLAME_RADIUS) continue
            sawPlayer = true
            if (!WardenSpawnTracker.isOnCooldown(player.uuid, now)) return true
        }
        return !sawPlayer
    }

    // <@AI_UNREVIEWED>
    private fun blameNearbyPlayers(
        ctx: UpdateContext,
        now: Long,
    ) {
        val block = ctx.block()
        for (player in ctx.instance.players) {
            if (player.position.distance(ctx.position.add(0.5, 0.5, 0.5)) > BLAME_RADIUS) continue
            if (WardenSpawnTracker.isOnCooldown(player.uuid, now)) continue

            val warningLevel = WardenSpawnTracker.blame(player.uuid, now)
            EventDispatcher.call(
                WardenBlameEvent(player, block, ctx.position.asBlockVec(), ctx.instance, warningLevel),
            )
        }
    }
    // </<@AI_UNREVIEWED>

    @Suppress("unused")
    private fun summonWarden(ctx: UpdateContext) {
        // todo: Not Implemented
        val warden = Entity(EntityType.WARDEN)
        warden.setInstance(ctx.instance, ctx.position.add(0.5, 0.0, 0.5))
    }

    private fun playShriek(ctx: UpdateContext) {
        ctx.instance.playSoundExcept(
            null,
            Sound.sound(SoundEvent.BLOCK_SCULK_SHRIEKER_SHRIEK, Sound.Source.BLOCK, 1.0f, 1.0f),
            ctx.position,
        )
    }

    private fun spawnShriekParticle(ctx: UpdateContext) {
        val particle = (Particle.SHRIEK as Particle.Shriek).withDelay(0)
        ctx.instance.sendGroupedPacket(
            ParticlePacket(particle, ctx.position.add(0.5, 0.5, 0.5), Vec.ZERO, 0.0f, 1),
        )
    }

    private fun applyDarkness(ctx: UpdateContext) {
        for (player in ctx.instance.players) {
            if (player.gameMode != GameMode.SURVIVAL && player.gameMode != GameMode.ADVENTURE) continue
            if (player.position.distance(ctx.position.add(0.5, 0.5, 0.5)) > DARKNESS_RADIUS) continue
            player.addEffect(Potion(PotionEffect.fromKey("minecraft:darkness")!!, 0, DARKNESS_TICKS, 0))
        }
    }

    private companion object {
        private const val SHRIEK_ACTION: Byte = 1
        private const val BLAME_RADIUS = 16.0
        private const val DETECTION_RADIUS = 8
        private const val SHRIEK_TICKS = 90
        private const val DARKNESS_RADIUS = 40.0
        private const val DARKNESS_TICKS = 240
    }
}
