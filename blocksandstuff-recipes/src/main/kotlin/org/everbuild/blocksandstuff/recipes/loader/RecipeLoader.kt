package org.everbuild.blocksandstuff.recipes.loader

import net.minestom.server.MinecraftServer.getRecipeManager
import org.everbuild.blocksandstuff.recipes.RecipeFactory
import org.everbuild.blocksandstuff.recipes.SerializationFactory
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.*
import java.util.stream.Stream

object RecipeLoader {

    private val logger = LoggerFactory.getLogger(RecipeLoader::class.java)

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

    private fun getResource(resource: String, module: Class<*>): URL {
        return module.getResource(resource) ?: File(resource).toURI().toURL()
    }

    internal fun loadNamespacedRecipeData(namespace: String, module: Class<*>): List<RecipeModel> {
        val recipes = mutableListOf<RecipeModel>()

        iterateResources("/data/$namespace/recipe/", module) { path ->
            if (!path.fileName.toString().endsWith(".json")) return@iterateResources
            val pathSpec = getResource(path.toString(), module)
            val file = File(pathSpec.file)
            if (!file.extension.equals("json", true)) return@iterateResources
            try {
                val value = SerializationFactory.json().decodeFromString<RecipeModel>(pathSpec.readText())
                recipes.add(value)
            } catch (e: Exception) {
                logger.warn("Could not load recipe ${path.fileName} from ${pathSpec.file}: ${e.message}")
            }
        }
        return recipes
    }

    fun loadAllRecipes(namespace: String) {
        val recipes = listOf(
            loadNamespacedRecipeData(namespace, RecipeLoader::class.java)
        ).flatten()
            .map { RecipeFactory.create(it) }

        for (recipe in recipes) {
            getRecipeManager().addRecipe(recipe)
        }
    }
}
