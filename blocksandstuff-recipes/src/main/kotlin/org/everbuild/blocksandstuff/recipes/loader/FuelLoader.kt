package org.everbuild.blocksandstuff.recipes.loader

import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.recipes.SerializationFactory
import java.io.File
import java.net.URL

object FuelLoader {
    var namespace: String = "everbuild"

    var furnaceFuels: List<FurnaceFuel>? = null

    private fun getResource(
        namespace: String,
        module: Class<*>,
    ): URL {
        this.namespace = namespace
        return module.getResource("/data/$namespace/fuels.json") ?: File("/data/$namespace/fuels.json").toURI().toURL()
    }

    fun loadAllFuels(namespace: String? = null): List<FurnaceFuel> {
        if (furnaceFuels != null) return furnaceFuels!!
        if (namespace != null) this.namespace = namespace

        val path: URL = getResource(this.namespace, FurnaceFuel::class.java)
        val value = SerializationFactory.json().decodeFromString<List<FurnaceFuel>>(path.readText())

        furnaceFuels = value
        return value
    }

    fun isFuel(item: ItemStack): Boolean = furnaceFuels?.any { it.itemStack.matches(item) } ?: false
}
