package org.everbuild.blocksandstuff.recipes.loader

import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.loot.number.ResolvableInt
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

    /**
     * An item is fuel when it carries the `cooking_fuel` component or is listed in
     * the JSON fuel list (which is still required because vanilla encodes the burn
     * time as an unresolved provider reference).
     */
    fun isFuel(item: ItemStack): Boolean =
        item.get(DataComponents.COOKING_FUEL) != null || furnaceFuels?.any { it.itemStack.matches(item) } == true

    fun burnTime(item: ItemStack): Int? {
        // Vanilla's component only references a burn-time provider (e.g.
        // "minecraft:cooking/time_coal"), so only literal values can be used directly.
        (item.get(DataComponents.COOKING_FUEL)?.burnTime() as? ResolvableInt.Constant)?.let { return it.value() }
        return furnaceFuels?.firstOrNull { it.itemStack.matches(item) }?.burnTime
    }
}
