package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.PacketSendingUtils
import java.util.Random

private const val STATE_PROPERTY = "potent_sulfur_state"
private const val STATE_DRY = "dry"
private const val STATE_WET = "wet"
private const val STATE_DORMANT = "dormant"
private const val STATE_ERUPTING = "erupting"
private const val STATE_CONTINUOUS = "continuous"
private const val MAX_WATER_COLUMN = 4

private val GEYSER_PLUME: Particle.GeyserPlume =
    Particle.fromKey("minecraft:geyser_plume") as? Particle.GeyserPlume
        ?: error("missing geyser plume particle")
private val NOXIOUS_GAS_CLOUD: Particle =
    Particle.fromKey("minecraft:noxious_gas_cloud")
        ?: error("missing noxious gas cloud particle")
private val NAUSEA: PotionEffect =
    PotionEffect.fromKey("minecraft:nausea")
        ?: error("missing nausea effect")

/**
 * Vanilla-like potent sulfur: bubbles/nausea under water, a periodic geyser above magma and a
 * continuous geyser above a lava source. Timing follows the vanilla block-entity tick and is
 * deliberately independent of the random-tick speed.
 */
class PotentSulfurHandler : BlockHandler {
    private var countdown = -1
    private var ticks = 0

    override fun getKey(): net.kyori.adventure.key.Key = net.kyori.adventure.key.Key.key("minecraft:potent_sulfur")

    override fun isTickable(): Boolean = true

    override fun tick(tick: BlockHandler.Tick) {
        val block = tick.block
        if (!block.compare(Block.POTENT_SULFUR)) return

        val instance = tick.instance
        val position = tick.blockPosition
        ticks++

        val water = waterColumn(instance, position)
        val state = block.getProperty(STATE_PROPERTY) ?: STATE_DRY

        if (water == 0) {
            updateState(instance, position, block, STATE_DRY)
            return
        }
        if (water > MAX_WATER_COLUMN) {
            updateState(instance, position, block, STATE_WET)
            return
        }

        val below = instance.getBlock(position.sub(0.0, 1.0, 0.0))
        when {
            isLavaSource(below) -> {
                updateState(instance, position, block, STATE_CONTINUOUS)
                if (ticks % 4 == 0) erupt(instance, position, water)
            }

            below.compare(Block.MAGMA_BLOCK) -> tickMagmaGeyser(instance, position, block, state, water)

            else -> {
                updateState(instance, position, block, STATE_WET)
                if (ticks % 10 == 0) applyNausea(instance, position, water)
                if (ticks % 20 == 0) spawnGasParticles(instance, position, water)
            }
        }
    }

    private fun tickMagmaGeyser(
        instance: Instance,
        position: Point,
        block: Block,
        state: String,
        water: Int,
    ) {
        if (state != STATE_DORMANT && state != STATE_ERUPTING) {
            setState(instance, position, block, STATE_DORMANT, dormantTicks(water, position))
            return
        }

        if (countdown < 0) {
            countdown = if (state == STATE_DORMANT) dormantTicks(water, position) else eruptionTicks(water, position)
        }
        if (state == STATE_ERUPTING) {
            if (ticks % 4 == 0) erupt(instance, position, water)
            if (ticks % 20 == 0) {
                instance.playSoundExcept(
                    null,
                    Sound.sound(SoundEvent.BLOCK_POTENT_SULFUR_GEYSER_ERUPTION_ACTIVE, Sound.Source.BLOCK, 1.0f, 1.0f),
                    position,
                )
            }
        } else {
            if (ticks % 10 == 0) applyNausea(instance, position, water)
            if (ticks % 20 == 0) spawnGasParticles(instance, position, water)
        }

        if (countdown > 0) {
            countdown--
            return
        }

        if (state == STATE_DORMANT) {
            setState(instance, position, block, STATE_ERUPTING, eruptionTicks(water, position))
            instance.playSoundExcept(
                null,
                Sound.sound(SoundEvent.BLOCK_POTENT_SULFUR_GEYSER_ERUPTION, Sound.Source.BLOCK, 1.0f, 1.0f),
                position,
            )
        } else {
            setState(instance, position, block, STATE_DORMANT, dormantTicks(water, position))
            instance.playSoundExcept(
                null,
                Sound.sound(SoundEvent.BLOCK_POTENT_SULFUR_GEYSER_CONTINUOUS_ERUPTION, Sound.Source.BLOCK, 1.0f, 1.0f),
                position,
            )
        }
    }

    private fun updateState(
        instance: Instance,
        position: Point,
        block: Block,
        state: String,
    ) {
        if (block.getProperty(STATE_PROPERTY) == state) return
        setState(instance, position, block, state, -1)
    }

    private fun setState(
        instance: Instance,
        position: Point,
        block: Block,
        state: String,
        nextCountdown: Int,
    ) {
        countdown = nextCountdown
        instance.setBlock(position, block.withProperty(STATE_PROPERTY, state))
    }

    private fun erupt(
        instance: Instance,
        position: Point,
        water: Int,
    ) {
        val height = 5 * water
        val center = position.add(0.5, 0.0, 0.5)
        val limit = 0.3 + water * 0.1
        for (entity in instance.getNearbyEntities(center, height.toDouble())) {
            if (!entity.isInGeyserColumn(position, height)) continue
            if (entity is net.minestom.server.entity.Player && entity.isFlying) continue
            val velocity = entity.velocity
            if (velocity.y() < limit) {
                entity.velocity = Vec(velocity.x(), limit, velocity.z())
            }
        }

        val surface = position.add(0.0, water.toDouble(), 0.0)
        PacketSendingUtils.sendPacket(
            instance,
            ParticlePacket(
                GEYSER_PLUME.withWaterBlocks(water),
                surface.x() + 0.5,
                surface.y() + 0.1,
                surface.z() + 0.5,
                0.1f,
                0.0f,
                0.1f,
                0.0f,
                4,
            ),
        )
    }

    private fun Entity.isInGeyserColumn(
        base: Point,
        height: Int,
    ): Boolean {
        val x = position.x()
        val y = position.y()
        val z = position.z()
        return x >= base.x() &&
            x < base.x() + 1.0 &&
            z >= base.z() &&
            z < base.z() + 1.0 &&
            y >= base.y() &&
            y <= base.y() + height
    }

    private fun applyNausea(
        instance: Instance,
        position: Point,
        water: Int,
    ) {
        val surface = position.add(0.0, water.toDouble(), 0.0)
        for (entity in instance.getNearbyEntities(surface.add(0.5, 0.0, 0.5), 3.0)) {
            entity.addEffect(Potion(NAUSEA, 0, 4 * 20))
        }
    }

    private fun spawnGasParticles(
        instance: Instance,
        position: Point,
        water: Int,
    ) {
        val surface = position.add(0.0, water.toDouble(), 0.0)
        PacketSendingUtils.sendPacket(
            instance,
            ParticlePacket(
                NOXIOUS_GAS_CLOUD,
                surface.x() + 0.5,
                surface.y() + 1.0,
                surface.z() + 0.5,
                0.1f,
                0.1f,
                0.1f,
                0.0f,
                3,
            ),
        )
    }

    private fun waterColumn(
        instance: Instance,
        position: Point,
    ): Int {
        var height = 0
        while (height <= MAX_WATER_COLUMN) {
            val block = instance.getBlock(position.add(0.0, (height + 1).toDouble(), 0.0))
            if (!isWaterSource(block)) break
            height++
        }
        return height
    }

    private fun isWaterSource(block: Block): Boolean =
        block.compare(Block.WATER) && (block.getProperty("level") ?: "0") == "0"

    private fun isLavaSource(block: Block): Boolean =
        block.compare(Block.LAVA) && (block.getProperty("level") ?: "0") == "0"

    private fun dormantTicks(
        water: Int,
        position: Point,
    ): Int {
        val random = Random(positionSeed(position))
        return 20 * (10 * (water - 1) + 15 + random.nextInt(16))
    }

    private fun eruptionTicks(
        water: Int,
        position: Point,
    ): Int {
        val random = Random(positionSeed(position) xor 0x5DEECE66DL)
        return 20 * ((water - 1) + 1 + random.nextInt(2))
    }

    private fun positionSeed(position: Point): Long =
        position.blockX() * 73856093L xor position.blockY() * 19349663L xor position.blockZ() * 83492791L
}
