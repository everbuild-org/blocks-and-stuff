package org.everbuild.blocksandstuff.recipes.loader

import net.minestom.server.MinecraftServer.getRecipeManager
import org.everbuild.blocksandstuff.recipes.RecipeFactory
import org.everbuild.blocksandstuff.recipes.SerializationFactory
import org.everbuild.blocksandstuff.common.utils.forEachResource
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Path

object RecipeLoader {

    private val logger = LoggerFactory.getLogger(RecipeLoader::class.java)

    var showErrors: Boolean = System.getProperty("blocksandstuff.recipes.showErrors")?.toBoolean() ?: false

    private fun getResource(resource: String, module: Class<*>): URL {
        return module.getResource(resource) ?: File(resource).toURI().toURL()
    }

    internal fun loadNamespacedRecipeData(namespace: String, module: Class<*>): List<RecipeModel> {
        val recipes = mutableListOf<RecipeModel>()
        var failures = 0

        forEachResource("/data/$namespace/recipe/", module) { path ->
            if (!path.fileName.toString().endsWith(".json")) return@forEachResource
            val pathSpec = getResource(path.toString(), module)
            val file = File(pathSpec.file)
            if (!file.extension.equals("json", true)) return@forEachResource
            try {
                val value = SerializationFactory.json().decodeFromString<RecipeModel>(pathSpec.readText())
                recipes.add(value)
            } catch (e: Exception) {
                failures++
                if (showErrors) {
                    logger.warn("Could not load recipe ${path.fileName} from ${pathSpec.file}: ${e.message}")
                }
            }
        }

        if (failures > 0 && !showErrors) {
            logger.warn(
                "Could not load $failures recipes from namespace '$namespace'. " +
                    "Set the system property 'blocksandstuff.recipes.showErrors=true' for details."
            )
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
