package org.everbuild.blocksandstuff.recipes.serializer.stack

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.ByteBinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.nbt.DoubleBinaryTag
import net.kyori.adventure.nbt.FloatBinaryTag
import net.kyori.adventure.nbt.IntBinaryTag
import net.kyori.adventure.nbt.ListBinaryTag
import net.kyori.adventure.nbt.LongBinaryTag
import net.kyori.adventure.nbt.StringBinaryTag

fun JsonPrimitive.asNbt(): BinaryTag {
    if (this is JsonNull) throw IllegalArgumentException("Can not convert json null to nbt")
    this.intOrNull?.let { return IntBinaryTag.intBinaryTag(it) }
    this.longOrNull?.let { return LongBinaryTag.longBinaryTag(it) }
    this.floatOrNull?.let { return FloatBinaryTag.floatBinaryTag(it) }
    this.doubleOrNull?.let { return DoubleBinaryTag.doubleBinaryTag(it) }
    this.booleanOrNull?.let { return if (it) ByteBinaryTag.ONE else ByteBinaryTag.ZERO }
    this.contentOrNull?.let { return StringBinaryTag.stringBinaryTag(it) }
    throw IllegalArgumentException("Can not convert json primitive to nbt")
}

fun JsonArray.asNbt(): BinaryTag {
    val builder = ListBinaryTag.builder()
    for (jsonElement in toList()) {
        jsonElement.asNbt().let { builder.add(it) }
    }
    return builder.build()
}

fun JsonObject.asNbt(): BinaryTag {
    val compound = CompoundBinaryTag.builder()
    for ((key, value) in this.entries) {
        compound.put(key, value.asNbt())
    }
    return compound.build()
}

fun JsonElement.asNbt(): BinaryTag {
    return when (this) {
        is JsonPrimitive -> this.asNbt()
        is JsonArray -> this.asNbt()
        is JsonObject -> this.asNbt()
    }
}