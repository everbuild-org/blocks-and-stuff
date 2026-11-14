package org.everbuild.blocksandstuff.common

import net.minestom.server.instance.Instance
import java.util.concurrent.ConcurrentHashMap

data class InstanceOptions(
    var randomTickSpeed: Int = 3,
    var redstoneEnabled: Boolean = true,
    var fireTick: Boolean = true,
    var tileDrops: Boolean = true,
    var daylightCycle: Boolean = true,
    var vinesSpread: Boolean = true,
    var playersSleepingPercentage: Int = 100,
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
