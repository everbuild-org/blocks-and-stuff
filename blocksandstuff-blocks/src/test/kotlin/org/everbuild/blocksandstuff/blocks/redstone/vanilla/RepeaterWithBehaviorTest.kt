package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.blocks.behavior.addRepeaters
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RepeaterWithBehaviorTest {
    private lateinit var instance: Instance
    private val base = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        RedstoneRegistrations.enable()
        RedstoneRegistrations.registry.registerVanillaComponents()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addRepeaters() })
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        RedstoneRegistrations.install(instance)
    }

    @AfterEach
    fun teardown() {
        RedstoneRegistrations.uninstall(instance)
        MinecraftServer.stopCleanly()
    }

    @Test
    fun repeaterStillPowersWithInteractionHandler() {
        val repeater = base.add(1.0, 0.0, 0.0)
        val lamp = base.add(2.0, 0.0, 0.0)

        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        instance.setBlock(repeater, Block.REPEATER.withProperty("facing", "west"))
        instance.setBlock(lamp, Block.REDSTONE_LAMP)

        val simulator = requireNotNull(RedstoneRegistrations.simulator(instance))
        repeat(6) { simulator.tick() }

        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }
}
