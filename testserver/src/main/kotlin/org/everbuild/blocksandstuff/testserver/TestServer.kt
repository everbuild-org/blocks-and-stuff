package org.everbuild.blocksandstuff.testserver

import java.io.File
import kotlin.system.exitProcess
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import org.everbuild.blocksandstuff.fluids.MinestomFluids
import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations
import org.everbuild.blocksandstuff.blocks.behavior.BlockBehaviorRuleRegistrations
import org.everbuild.blocksandstuff.blocks.group.VanillaPlacementRules

class TestServer(generateElements: Boolean) {
    private val server: MinecraftServer = MinecraftServer.init()

    init {
        val instance: Instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator { unit: GenerationUnit ->
            unit.modifier().fillHeight(0, 65, Block.GRASS_BLOCK)
        }

        BlockPlacementRuleRegistrations.registerDefault()
        BlockBehaviorRuleRegistrations.registerDefault()
        MinestomFluids.enableFluids()
        MinestomFluids.enableVanillaFluids()

        if (generateElements) {
            val allPlacementRuleBlockKeys = VanillaPlacementRules.ALL
                .flatMap { it.blockGroup.allMatching() }
                .map { it.key().asString() }

            File("../.github/list-producer/supported-blocks.txt").writeText(allPlacementRuleBlockKeys.joinToString("\n"))
            println("Written ${allPlacementRuleBlockKeys.size} block keys to file .github/list-producer/supported-blocks.txt")
            exitProcess(0)
        }

        MinecraftServer.getGlobalEventHandler().addListener(
            AsyncPlayerConfigurationEvent::class.java
        ) { event: AsyncPlayerConfigurationEvent ->
            event.spawningInstance = instance
            event.player.respawnPoint = Pos(0.0, 65.0, 0.0)
            event.player.setGameMode(GameMode.CREATIVE)
        }
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
