package org.everbuild.blocksandstuff.redstone

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceBlockUpdateEvent
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceChunkUnloadEvent
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.event.instance.InstanceUnregisterEvent
import net.minestom.server.instance.Instance
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider
import org.everbuild.blocksandstuff.redstone.engine.RedstoneSimulator
import java.util.concurrent.ConcurrentHashMap

/**
 * Entry point for the redstone engine.
 *
 * The [registry] is global (component types), while simulators are per instance. [enable] wires the
 * global listeners; whether an instance actually simulates is decided by [filter].
 */
object RedstoneRegistrations {
    val registry: RedstoneRegistry = RedstoneRegistry()

    private val simulators = ConcurrentHashMap<Instance, RedstoneSimulator>()
    private var registeredHandler: Any? = null
    private var instanceFilter: (Instance) -> Boolean = { InstanceOptionsProvider.getForInstance(it).redstoneEnabled }

    @JvmStatic
    fun enable(filter: (Instance) -> Boolean = { InstanceOptionsProvider.getForInstance(it).redstoneEnabled }) {
        instanceFilter = filter

        val handler = MinecraftServer.getGlobalEventHandler()
        if (registeredHandler === handler) return
        registeredHandler = handler

        val node = EventNode.all("blocksandstuff:redstone")
        node.addListener(InstanceTickEvent::class.java) { event ->
            simulatorOrNull(event.instance)?.tick()
        }
        node.addListener(InstanceBlockUpdateEvent::class.java) { event ->
            simulatorOrNull(event.instance)?.onBlockChanged(event.blockPosition)
        }
        node.addListener(InstanceChunkLoadEvent::class.java) { event ->
            simulatorOrNull(event.instance)?.onChunkLoaded(event.chunk)
        }
        node.addListener(InstanceChunkUnloadEvent::class.java) { event ->
            simulator(event.instance)?.onChunkUnloaded(event.chunk)
        }
        node.addListener(InstanceUnregisterEvent::class.java) { event ->
            simulators.remove(event.instance)
            InstanceOptionsProvider.remove(event.instance)
        }
        handler.addChild(node)
    }

    @JvmStatic
    fun install(instance: Instance): RedstoneSimulator =
        simulators.computeIfAbsent(instance) { RedstoneSimulator(instance, registry) }

    @JvmStatic
    fun uninstall(instance: Instance) {
        simulators.remove(instance)
    }

    @JvmStatic
    fun simulator(instance: Instance): RedstoneSimulator? = simulators[instance]

    /** Re-evaluates redstone after a silent server-side block write that did not dispatch an event. */
    @JvmStatic
    fun notifyBlockChanged(
        instance: Instance,
        position: Point,
        delayTicks: Int = 1,
    ) {
        simulator(instance)?.onBlockChanged(position, delayTicks)
    }

    /** Re-evaluates redstone around a block whose inventory contents changed (e.g. comparator input). */
    @JvmStatic
    fun notifyContainerChanged(
        instance: Instance,
        position: Point,
    ) {
        simulator(instance)?.onBlockChanged(position)
    }

    private fun simulatorOrNull(instance: Instance): RedstoneSimulator? {
        if (!instanceFilter(instance)) return null
        simulator(instance)?.let { return it }
        return install(instance)
    }
}
