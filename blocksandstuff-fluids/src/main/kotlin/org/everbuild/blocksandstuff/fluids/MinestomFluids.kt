package org.everbuild.blocksandstuff.fluids

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import org.everbuild.averium.worlds.fluid.EmptyFluid
import org.everbuild.averium.worlds.fluid.Fluid
import org.everbuild.averium.worlds.fluid.listener.setupFluidPickupEvent
import org.everbuild.blocksandstuff.fluids.listener.setupFluidPlacementEvent
import java.util.concurrent.ConcurrentHashMap

object MinestomFluids {

    val WATER: Fluid = WaterFluid(Block.WATER, Material.WATER_BUCKET)
    val LAVA: Fluid = LavaFluid(Block.LAVA, Material.LAVA_BUCKET)
    val EMPTY: Fluid = EmptyFluid()

    val UPDATES: MutableMap<Instance, MutableMap<Long, MutableSet<Point>>> = ConcurrentHashMap()

    fun get(block: Block): Fluid {
        return if (block.compare(Block.WATER)) {
            WATER
        } else if (block.compare(Block.LAVA)) {
            LAVA
        } else {
            EMPTY
        }
    }

    fun tick(event: InstanceTickEvent) {
        val currentUpdate: Set<Point>? =
            UPDATES.computeIfAbsent(event.instance) { i: Instance? -> ConcurrentHashMap<Long, MutableSet<Point>>() }[event.instance.worldAge]
        if (currentUpdate == null) return
        for (point in currentUpdate) {
            tick1(event.instance, point)
        }
        UPDATES[event.instance]!!.remove(event.instance.worldAge)
    }

    fun tick1(instance: Instance, point: Point) {
        val block = instance.getBlock(point)
        val fluid = get(block)

        // Process fluid behavior
        fluid.onTick(instance, point, block)

        // Schedule the next tick to ensure continuous flow
        scheduleTick(instance, point, block)
    }

    fun scheduleTick(instance: Instance, point: Point, block: Block) {
        val tickDelay = get(block).getNextTickDelay(instance, point, block)
        if (tickDelay == -1) return

        val newAge = instance.worldAge + tickDelay

        // Ensure the instance exists in UPDATES
        UPDATES.computeIfAbsent(instance) { i: Instance? -> ConcurrentHashMap<Long, MutableSet<Point>>() }
            .computeIfAbsent(newAge) { _: Long? -> HashSet() }
            .add(point)
    }


    private fun init() {
        MinecraftServer.getBlockManager().registerBlockPlacementRule(FluidPlacementRule(Block.WATER))
        MinecraftServer.getBlockManager().registerBlockPlacementRule(FluidPlacementRule(Block.AIR))
    }

    private fun events(): EventNode<Event> {
        val node = EventNode.all("fluid-events")
        node.addListener(InstanceTickEvent::class.java, MinestomFluids::tick)
        setupFluidPlacementEvent()
        setupFluidPickupEvent()
        return node
    }

    @JvmStatic
    fun registerFluids() {
        init()
        MinecraftServer.getGlobalEventHandler().addChild(events())
    }
}