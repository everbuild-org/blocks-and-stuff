package org.everbuild.blocksandstuff.common.utils

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Visits files under a classpath resource directory in either an exploded or JAR resource. */
fun forEachResource(
    resourceDir: String,
    module: Class<*>,
    action: (Path) -> Unit,
) {
    val uri = module.getResource(resourceDir)?.toURI() ?: error("Cannot find resources")
    if (uri.scheme == "jar") {
        FileSystems.newFileSystem(uri, emptyMap<String, Any>()).use { fileSystem ->
            Files.walk(fileSystem.getPath(resourceDir)).use { paths -> paths.forEach(action) }
        }
    } else {
        Files.walk(Paths.get(uri)).use { paths -> paths.forEach(action) }
    }
}
