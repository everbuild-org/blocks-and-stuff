package org.everbuild.blocksandstuff.testserver

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.GameMode
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.LightingChunk
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.utils.chunk.ChunkSupplier
import org.everbuild.blocksandstuff.blocks.BlockBehaviorRuleRegistrations
import org.everbuild.blocksandstuff.blocks.BlockPickup
import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations
import org.everbuild.blocksandstuff.blocks.PlacedHandlerRegistration
import org.everbuild.blocksandstuff.blocks.group.VanillaPlacementRules
import org.everbuild.blocksandstuff.fluids.MinestomFluids
import java.io.File
import kotlin.system.exitProcess

class TestServer(
    generateElements: Boolean,
) {
    private val server: MinecraftServer = MinecraftServer.init()
    private val ingestRb = Array<Long>(500) { 0 }
    private var ingestCount = 0

    init {
        val instance: Instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.chunkSupplier = ChunkSupplier { inst, x, z -> LightingChunk(inst, x, z) }
        instance.setGenerator(ExampleGenerator())

        BlockPlacementRuleRegistrations.registerDefault()
        BlockBehaviorRuleRegistrations.registerDefault()
        PlacedHandlerRegistration.registerDefault()
        BlockPickup.enable()
        MinestomFluids.enableFluids()
        MinestomFluids.enableVanillaFluids()
        // MinestomFluids.enableAutoIngestion() -- use this. for perf debugging purposes, this is done manually

        MinecraftServer
            .getGlobalEventHandler()
            .addListener(InstanceChunkLoadEvent::class.java) {
                val before = System.nanoTime()
                MinestomFluids.ingestChunk(it.instance, it.chunk)
                val after = System.nanoTime()
                ingestRb[ingestCount++ % ingestRb.size] = after - before
                if (ingestCount >= 500) {
                    println("Average ingest time: ${ingestRb.average() / 1000000} ms")
                    ingestCount = 0
                }
            }

        if (generateElements) {
            val allPlacementRuleBlockKeys =
                VanillaPlacementRules.ALL
                    .flatMap { it.blockGroup.allMatching() }
                    .map { it.key().asString() }

            File("../.github/list-producer/supported-blocks.txt").writeText(allPlacementRuleBlockKeys.joinToString("\n"))
            println("Written ${allPlacementRuleBlockKeys.size} block keys to file .github/list-producer/supported-blocks.txt")
            exitProcess(0)
        }

        MinecraftServer.getGlobalEventHandler().addListener(
            AsyncPlayerConfigurationEvent::class.java,
        ) { event: AsyncPlayerConfigurationEvent ->
            event.spawningInstance = instance
            event.player.respawnPoint = Pos(0.0, 65.0, 0.0)
            event.player.setGameMode(GameMode.SURVIVAL)
            event.player.inventory.setEquipment(EquipmentSlot.MAIN_HAND, 1, ItemStack.of(Material.WATER_BUCKET))
        }

        MinecraftServer.getCommandManager().register(DebugCommand())
    }

    fun bind() {
        server.start("0.0.0.0", 25565)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TestServer(args.contains("gen-elements")).bind()
        }
    }
}
