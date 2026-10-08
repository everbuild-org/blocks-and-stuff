package org.everbuild.blocksandstuff.recipes.util

import net.minestom.server.item.Material
import net.minestom.server.registry.TagKey

fun Material.isIn(tag: TagKey<Material>): Boolean =
    Material.staticRegistry().getTag(tag)?.any { it.key() == key() } ?: false
