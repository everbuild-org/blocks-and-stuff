package org.everbuild.blocksandstuff.redstone.engine

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.redstone.PowerSource
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RedstoneSimulatorTest {
    private lateinit var instance: Instance
    private lateinit var simulator: RedstoneSimulator
    private val base = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        RedstoneRegistrations.enable()
        RedstoneRegistrations.registry.source(
            Block.REDSTONE_BLOCK,
            object : PowerSource {
                override fun strength(block: Block): Int = 15
            },
        )
        RedstoneRegistrations.registry.consumer(Block.REDSTONE_LAMP) { ctx, powered, _ ->
            ctx.block().withProperty("lit", powered.toString())
        }

        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        simulator = RedstoneRegistrations.install(instance)
    }

    @AfterEach
    fun teardown() {
        RedstoneRegistrations.uninstall(instance)
        MinecraftServer.stopCleanly()
    }

    @Test
    fun sourcePowersAdjacentConsumer() {
        val lamp = base.add(1.0, 0.0, 0.0)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        instance.setBlock(base, Block.REDSTONE_BLOCK)

        simulator.tick()

        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun removingSourceUnpowersConsumer() {
        val lamp = base.add(1.0, 0.0, 0.0)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        instance.setBlock(base, Block.REDSTONE_BLOCK)
        simulator.tick()
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))

        instance.setBlock(base, Block.AIR)
        simulator.tick()

        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun manualSchedulingPowersConsumer() {
        val lamp = base.add(1.0, 0.0, 0.0)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        instance.setBlock(base, Block.REDSTONE_BLOCK)
        simulator.onBlockChanged(lamp)
        simulator.onBlockChanged(base)

        Assertions.assertEquals(15, simulator.signalInto(lamp))

        simulator.tick()
        simulator.tick()

        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun weakSignalMatchesSourceStrength() {
        instance.setBlock(base, Block.REDSTONE_BLOCK)

        Assertions.assertEquals(15, simulator.weakSignal(base, net.minestom.server.instance.block.BlockFace.EAST))
    }
}
