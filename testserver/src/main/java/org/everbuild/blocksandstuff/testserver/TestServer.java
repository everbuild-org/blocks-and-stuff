package org.everbuild.blocksandstuff.testserver;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations;

public class TestServer {
    private final MinecraftServer server = MinecraftServer.init();

    public TestServer() {
        Instance instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 65, Block.GRASS_BLOCK));

        BlockPlacementRuleRegistrations.registerDefault();

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 65, 0));
            event.getPlayer().setGameMode(GameMode.CREATIVE);
        });
    }

    public void bind() {
        server.start("0.0.0.0", 25565);
    }

    public static void main(String[] args) {
        new TestServer().bind();
    }
}
