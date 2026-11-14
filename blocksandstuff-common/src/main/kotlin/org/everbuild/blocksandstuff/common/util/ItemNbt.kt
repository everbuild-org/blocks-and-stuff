package org.everbuild.blocksandstuff.common.util

import net.kyori.adventure.nbt.BinaryTag
import net.minestom.server.MinecraftServer
import net.minestom.server.codec.Transcoder
import net.minestom.server.registry.RegistryTranscoder

/** NBT transcoder with registries, required for item components such as stored enchantments. */
fun itemNbtTranscoder(): Transcoder<BinaryTag> =
    RegistryTranscoder(Transcoder.NBT, MinecraftServer.getRegistries())
