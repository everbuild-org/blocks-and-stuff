package org.everbuild.blocksandstuff.common.tag

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.nio.file.Files
import org.everbuild.blocksandstuff.common.utils.forEachResource

object TagRegistryLoader {
    private val gson = Gson()

    fun loadTags(folder: String, type: String, module: Class<*>): Map<String, Set<String>> {
        val map = hashMapOf<String, HashSet<String>>()

        forEachResource("/$folder/$type/", module) { path ->
            if (!path.fileName.toString().endsWith(".json")) return@forEachResource
            val value = gson.fromJson(Files.readString(path), JsonObject::class.java)
            val name = value.get("tag").asString
            val values = value.getAsJsonArray("contents").map { it.asString }.toSet()

            map.getOrPut(name) { hashSetOf() }.addAll(values)
        }
        return map
    }
}
