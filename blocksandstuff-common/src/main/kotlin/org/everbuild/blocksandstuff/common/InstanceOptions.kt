package org.everbuild.blocksandstuff.common

import net.minestom.server.instance.Instance
import java.util.concurrent.ConcurrentHashMap

data class InstanceOptions(
    var randomTickSpeed: Int = 3,
    var redstoneEnabled: Boolean = true,
)

object InstanceOptionsProvider {
    private val default = InstanceOptions()
    private val perInstance = ConcurrentHashMap<Instance, InstanceOptions>()

    @JvmStatic
    fun getForInstance(instance: Instance) = perInstance.computeIfAbsent(instance) { default.copy() }

    @JvmStatic
    fun remove(instance: Instance) {
        perInstance.remove(instance)
    }
}
