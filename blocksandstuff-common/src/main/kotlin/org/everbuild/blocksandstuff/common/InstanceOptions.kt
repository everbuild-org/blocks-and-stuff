package org.everbuild.blocksandstuff.common

import net.minestom.server.instance.Instance

data class InstanceOptions(
    var randomTickSpeed: Int = 3,
    var redstoneEnabled: Boolean = true,
)

object InstanceOptionsProvider {
    private val default = InstanceOptions()
    private val perInstance = mutableMapOf<Instance, InstanceOptions>()

    @JvmStatic
    fun getForInstance(instance: Instance) = perInstance.getOrPut(instance) { default }

    @JvmStatic
    fun remove(instance: Instance) {
        perInstance.remove(instance)
    }
}