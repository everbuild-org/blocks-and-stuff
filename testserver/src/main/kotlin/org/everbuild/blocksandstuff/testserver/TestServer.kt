package org.everbuild.blocksandstuff.testserver

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import org.everbuild.blocksandstuff.fluids.MinestomFluids
import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations

class TestServer {
    private val server: MinecraftServer = MinecraftServer.init()

    init {
        val instance: Instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator { unit: GenerationUnit ->
            unit.modifier().fillHeight(0, 65, Block.GRASS_BLOCK)
        }

        BlockPlacementRuleRegistrations.registerDefault()
        MinestomFluids.registerFluids()

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
            TestServer().bind()
        }
    }
}
