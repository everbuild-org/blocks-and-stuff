package org.everbuild.blocksandstuff.blocks.util

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.PrimedTntMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

/**
 * Primed TNT entity. Counts down its fuse and then performs a simple spherical explosion (blocks are
 * removed, no drops). Explosion resistance and knockback are not modelled.
 */
class PrimedTnt(
    private val fuseTicks: Int = DEFAULT_FUSE,
) : Entity(EntityType.TNT) {
    private var remaining = fuseTicks

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

        for (dx in -RADIUS..RADIUS) {
            for (dy in -RADIUS..RADIUS) {
                for (dz in -RADIUS..RADIUS) {
                    if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) continue
                    val target = center.add(dx.toDouble(), dy.toDouble(), dz.toDouble())
                    val block = instance.getBlock(target)
                    if (block.air() || block.key() in unbreakableKeys) continue
                    instance.setBlock(target, Block.AIR)
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_FUSE = 80
        private const val RADIUS = 4

        private val unbreakableKeys: Set<Key> = setOf(
            Key.key("minecraft:bedrock"),
            Key.key("minecraft:barrier"),
            Key.key("minecraft:obsidian"),
            Key.key("minecraft:crying_obsidian"),
            Key.key("minecraft:end_portal"),
            Key.key("minecraft:end_portal_frame"),
            Key.key("minecraft:reinforced_deepslate"),
        )

        fun spawn(
            instance: Instance,
            position: Point,
        ): PrimedTnt =
            PrimedTnt().also {
                it.setInstance(instance, position.add(0.5, 0.5, 0.5))
            }
    }
}
