package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key

/** Dropper: nine-slot storage that ejects a random item in its facing direction on a redstone pulse. */
class DropperHandler(
    key: Key,
    archetype: ContainerArchetype,
) : EjectorHandler(key, archetype)
