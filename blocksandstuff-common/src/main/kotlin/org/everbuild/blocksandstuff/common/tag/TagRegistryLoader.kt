package org.everbuild.blocksandstuff.common.tag

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream


object TagRegistryLoader {
    private val gson = Gson()

    private fun iterateResources(resourceDir: String, module: Class<*>, action: (Path) -> Unit) {
        val uri: URI = module.getResource(resourceDir)?.toURI() ?: error("Cannot find resources")
        val myPath: Path
        if (uri.scheme.equals("jar")) {
            val fileSystem: FileSystem = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            myPath = fileSystem.getPath(resourceDir)
        } else {
            myPath = Paths.get(uri)
        }
        val walk: Stream<Path> = Files.walk(myPath)
        walk.forEach(action)
    }


    fun loadTags(folder: String, type: String, module: Class<*>): Map<String, Set<String>> {
        val map = hashMapOf<String, HashSet<String>>()

        iterateResources("/$folder/$type/", module) { path ->
            if (!path.fileName.toString().endsWith(".json")) return@iterateResources
            val pathSpec = module.getResource(path.toString())!!
            val file = File(pathSpec.file)
            if (!file.extension.equals("json", true)) return@iterateResources
            val value = gson.fromJson(pathSpec.readText(), JsonObject::class.java)
            val name = value.get("tag").asString
            val values = value.getAsJsonArray("contents").map { it.asString }.toSet()

            map.getOrPut(name) { hashSetOf() }.addAll(values)
        }
        return map
    }
}