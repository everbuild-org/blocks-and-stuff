package org.everbuild.blocksandstuff.blocks.util

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

private const val TNT_CHAIN_FUSE = 10

private val UNBREAKABLE_KEYS =
    setOf(
        "minecraft:bedrock",
        "minecraft:barrier",
        "minecraft:obsidian",
        "minecraft:crying_obsidian",
        "minecraft:end_portal",
        "minecraft:end_portal_frame",
        "minecraft:reinforced_deepslate",
    )

fun explodeBlocks(
    instance: Instance,
    center: Point,
    radius: Int,
    primeTnt: Boolean = true,
) {
    for (dx in -radius..radius) {
        for (dy in -radius..radius) {
            for (dz in -radius..radius) {
                if (dx * dx + dy * dy + dz * dz > radius * radius) continue

                val target = center.add(dx.toDouble(), dy.toDouble(), dz.toDouble())
                val block = instance.getBlock(target)
                if (block.air() || block.key().asString() in UNBREAKABLE_KEYS) continue

                if (primeTnt && block.compare(Block.TNT)) {
                    PrimedTnt.spawn(instance, target, TNT_CHAIN_FUSE)
                }
                instance.setBlock(target, Block.AIR)
            }
        }
    }
}
