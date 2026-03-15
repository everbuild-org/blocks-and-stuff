package org.everbuild.blocksandstuff.testserver

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.GameMode
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerGameModeRequestEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.LightingChunk
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.client.play.ClientClickWindowButtonPacket
import net.minestom.server.utils.chunk.ChunkSupplier
import org.everbuild.averium.org.everbuild.blocksandstuff.recipes.impl.StashControllerImpl
import org.everbuild.blocksandstuff.blocks.BlockBehaviorRuleRegistrations
import org.everbuild.blocksandstuff.blocks.BlockPickup
import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations
import org.everbuild.blocksandstuff.blocks.PlacedHandlerRegistration
import org.everbuild.blocksandstuff.blocks.group.VanillaPlacementRules
import org.everbuild.blocksandstuff.fluids.MinestomFluids
import org.everbuild.blocksandstuff.recipes.RecipeFactory
import org.everbuild.blocksandstuff.recipes.grid.CraftingTableHandler
import org.everbuild.blocksandstuff.recipes.grid.PlayerInventoryCraftingGridService
import org.everbuild.blocksandstuff.recipes.impl.ItemControllerImpl
import org.everbuild.blocksandstuff.recipes.loader.FuelLoader
import org.everbuild.blocksandstuff.recipes.loader.RecipeLoader
import org.everbuild.blocksandstuff.recipes.smelting.FurnaceInventory
import org.everbuild.blocksandstuff.recipes.smelting.blast_furnace.BlastFurnaceHandler
import org.everbuild.blocksandstuff.recipes.smelting.furnace.FurnaceHandler
import org.everbuild.blocksandstuff.recipes.smelting.smoker.SmokerHandler
import org.everbuild.blocksandstuff.recipes.smithing.SmithingTableHandler
import org.everbuild.blocksandstuff.recipes.stonecutting.StonecutterHandler
import org.everbuild.blocksandstuff.recipes.util.InventoryButtonClickListener
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
        loadRecipes()
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

        MinecraftServer.getGlobalEventHandler().addChild(
            EventNode.all("test-server-ux")
                .addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
                    event.spawningInstance = instance
                    event.player.respawnPoint = Pos(0.0, 65.0, 0.0)
                    event.player.gameMode = GameMode.SURVIVAL
                    event.player.inventory.setEquipment(EquipmentSlot.MAIN_HAND, 1, ItemStack.of(Material.WATER_BUCKET))
                    event.player.permissionLevel = 4
                }
                .addListener(PlayerGameModeRequestEvent::class.java) { event ->
                    event.player.gameMode = event.requestedGameMode
                }
        )

        MinecraftServer.getCommandManager().register(DebugCommand())
    }

    fun bind() {
        server.start("0.0.0.0", 25565)
    }

    fun loadRecipes() {
        RecipeFactory.itemController = ItemControllerImpl
        RecipeFactory.stashController = StashControllerImpl
        RecipeLoader.loadAllRecipes("everbuild")
        FuelLoader.loadAllFuels("everbuild")
        FurnaceInventory.withInventoryReload()

        MinecraftServer.getGlobalEventHandler().addListener(
            PlayerSpawnEvent::class.java
        ) {
            PlayerInventoryCraftingGridService(it.player)
        }

        MinecraftServer.getBlockManager().registerHandler(
            "minecraft:crafting_table"
        ) {
            CraftingTableHandler()
        }
        MinecraftServer.getBlockManager().registerHandler(
            "minecraft:smithing_table"
        ) {
            SmithingTableHandler()
        }
        MinecraftServer.getBlockManager().registerHandler(
            "minecraft:furnace"
        ) {
            FurnaceHandler()
        }
        MinecraftServer.getBlockManager().registerHandler(
            "minecraft:blast_furnace"
        ) {
            BlastFurnaceHandler()
        }
        MinecraftServer.getBlockManager().registerHandler(
            "minecraft:smoker"
        ) {
            SmokerHandler()
        }
        MinecraftServer.getBlockManager().registerHandler(
            "minecraft:stonecutter"
        ) {
            StonecutterHandler()
        }

        MinecraftServer.getPacketListenerManager().setPlayListener(
            ClientClickWindowButtonPacket::class.java,
            InventoryButtonClickListener::inventoryButtonClickListener
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TestServer(args.contains("gen-elements")).bind()
        }
    }
}
