package org.everbuild.blocksandstuff.blocks.util

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.sound.SoundEvent
import java.util.Random

/**
 * Primed TNT entity. Counts down its fuse and then performs a simple spherical explosion (blocks are
 * removed, no drops). Explosion resistance and knockback are not modelled.
 */
class PrimedTnt(
    private val fuseTicks: Int = DEFAULT_FUSE,
) : Entity(EntityType.TNT) {
    private var remaining = fuseTicks
    private val random = Random()

    init {
        editEntityMeta(PrimedTntMeta::class.java) {
            it.fuseTime = fuseTicks
            it.blockState = Block.TNT
        }
    }

    override fun tick(time: Long) {
        super.tick(time)
        remaining--
        if (remaining <= 0) {
            explode()
            remove()
            return
        }
        editEntityMeta(PrimedTntMeta::class.java) { it.fuseTime = remaining }
    }

    private fun explode() {
        val instance = this.instance ?: return
        val center = position.asBlockVec()

        playExplosionEffects(instance)
        explodeBlocks(instance, center, RADIUS)
    }

    private fun playExplosionEffects(instance: Instance) {
        val pitch = (1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f) * 0.7f
        instance.playSoundExcept(
            null,
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.BLOCK, 4.0f, pitch),
            position,
        )
        instance.sendGroupedPacket(
            ParticlePacket(Particle.EXPLOSION_EMITTER, position, Vec.ZERO, 0.0f, 1),
        )
    }

    companion object {
        private const val DEFAULT_FUSE = 80
        private const val RADIUS = 4

        fun spawn(
            instance: Instance,
            position: Point,
        ): PrimedTnt = spawn(instance, position, DEFAULT_FUSE)

        fun spawn(
            instance: Instance,
            position: Point,
            fuseTicks: Int,
        ): PrimedTnt =
            PrimedTnt(fuseTicks).also {
                it.setInstance(instance, position.add(0.5, 0.5, 0.5))
                instance.playSoundExcept(
                    null,
                    Sound.sound(SoundEvent.ENTITY_TNT_PRIMED, Sound.Source.BLOCK, 1.0f, 1.0f),
                    position,
                )
            }
    }
}
