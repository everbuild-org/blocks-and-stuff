package org.everbuild.blocksandstuff.fluids

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.instance.Chunk
import net.minestom.server.registry.DynamicRegistry
import net.minestom.server.registry.RegistryKey
import org.everbuild.blocksandstuff.fluids.impl.EmptyFluid
import org.everbuild.blocksandstuff.fluids.impl.Fluid
import org.everbuild.blocksandstuff.fluids.impl.LavaFluid
import org.everbuild.blocksandstuff.fluids.impl.WaterFluid
import org.everbuild.blocksandstuff.fluids.placement.getFluidPlacementEventNode
import org.everbuild.blocksandstuff.fluids.pickup.getFluidPickupEventNode

object MinestomFluids {
    private var enabled = false
    val UPDATES: MutableMap<Instance, MutableMap<Long, MutableSet<Point>>> = ConcurrentHashMap()

    val registry = DynamicRegistry.create<Fluid>(Key.key("blocksandstuff:fluids"))
        @JvmStatic get

    val EMPTY = registry.register("minecraft:empty", EmptyFluid())

    fun getFluidOnBlock(block: Block): RegistryKey<Fluid> {
        return registry.values().firstOrNull { it.isInTile(block) }?.let { registry.getKey(it) } ?: EMPTY
    }

    fun getFluidInstanceOnBlock(block: Block): Fluid {
        return registry.values().firstOrNull { it.isInTile(block) } ?: registry[EMPTY]!!
    }

    fun onTick(event: InstanceTickEvent) {
        val currentUpdate: Set<Point>? = UPDATES
            .computeIfAbsent(event.instance) {
                ConcurrentHashMap<Long, MutableSet<Point>>()
            }[event.instance.worldAge]

        if (currentUpdate == null) return

        for (point in currentUpdate) {
            processFluidTick(event.instance, point)
        }

        UPDATES[event.instance]!!.remove(event.instance.worldAge)
    }

    fun processFluidTick(instance: Instance, point: Point) {
        val block = instance.getBlock(point)
        val fluid = registry[getFluidOnBlock(block)]!!

        fluid.onTick(instance, point, block)
        scheduleTick(instance, point, block)
    }

    fun scheduleTick(instance: Instance, point: Point, block: Block) {
        val tickDelay = registry[getFluidOnBlock(block)]!!
            .getNextTickDelay(instance, point, block)

        if (tickDelay == -1) return

        val newAge = instance.worldAge + tickDelay
        UPDATES.computeIfAbsent(instance) { ConcurrentHashMap<Long, MutableSet<Point>>() }
            .computeIfAbsent(newAge) { HashSet() }
            .add(point)
    }

    private fun events(): EventNode<Event> = EventNode.all("fluid-events")
        .addListener(InstanceTickEvent::class.java, MinestomFluids::onTick)
        .addChild(getFluidPickupEventNode())
        .addChild(getFluidPlacementEventNode())

    /**
     * Processes the given chunk to ingest and manage fluid-related operations,
     * enabling fluid mechanics within the specified chunk.
     *
     * This is needed due to the way water updating is initially triggered.
     *
     * Call this whenever you manually edit a chunk. It is automatically called using world generation / loading
     *
     * @param chunk The chunk to be processed for fluid-related mechanics.
     */
    @JvmStatic
    fun ingestChunk(instance: Instance, chunk: Chunk) {
        val minY = instance.cachedDimensionType.minY()
        val startX = chunk.chunkX * 16
        val startZ = chunk.chunkZ * 16
        chunk.sections.forEachIndexed { i, section ->
            val palette = section.blockPalette()
            for (x in 0 until Chunk.CHUNK_SIZE_X) {
                for (z in 0 until Chunk.CHUNK_SIZE_Z) {
                    for (sectionRelY in 0 until Chunk.CHUNK_SECTION_SIZE) {
                        val y = (i * 16 + sectionRelY)
                        val blockId = palette.get(x, y, z)
                        if (!FluidBlockCache.BLOCK_STATES.contains(blockId)) {
                            continue
                        }

                        scheduleTick(
                            instance,
                            BlockVec(x + startX, y + minY, z + startZ),
                            Block.fromStateId(blockId)!!,
                        )
                    }
                }
            }
        }
    }

    @JvmStatic
    fun enableFluids() {
        if (enabled) return
        enabled = true
        MinecraftServer.getBlockManager().registerBlockPlacementRule(FluidPlacementRule(Block.WATER))
        MinecraftServer.getBlockManager().registerBlockPlacementRule(FluidPlacementRule(Block.LAVA))
        MinecraftServer.getBlockManager().registerBlockPlacementRule(FluidPlacementRule(Block.AIR))
        MinecraftServer.getGlobalEventHandler().addChild(events())
    }

    @JvmStatic
    fun enableVanillaFluids() {
        if (!enabled) enableFluids()
        registry.register("minecraft:water", WaterFluid(Block.WATER, Material.WATER_BUCKET))
        registry.register("minecraft:lava", LavaFluid(Block.LAVA, Material.LAVA_BUCKET))
    }

    @JvmStatic
    fun enableAutoIngestion() {
        events()
            .addListener(InstanceChunkLoadEvent::class.java) {
                ingestChunk(it.instance, it.chunk)
            }
    }
}