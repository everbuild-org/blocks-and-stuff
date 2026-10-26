package org.everbuild.blocksandstuff.redstone

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.InstanceOptionsProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RedstoneToggleTest {
    private lateinit var instance: Instance
    private val base = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        RedstoneRegistrations.enable()
        RedstoneRegistrations.registry.source(
            Block.REDSTONE_BLOCK,
            object : PowerSource {
                override fun strength(block: Block): Int = REDSTONE_MAX_POWER
            },
        )
        RedstoneRegistrations.registry.consumer(Block.REDSTONE_LAMP) { ctx, powered, _ ->
            ctx.block().withProperty("lit", powered.toString())
        }

        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun disabledInstanceIsNotSimulated() {
        InstanceOptionsProvider.getForInstance(instance).redstoneEnabled = false
        instance.loadChunk(0, 0).join()
        val lamp = base.add(1.0, 0.0, 0.0)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        instance.setBlock(base, Block.REDSTONE_BLOCK)

        Assertions.assertNull(RedstoneRegistrations.simulator(instance))
        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun enabledInstanceIsSimulated() {
        instance.loadChunk(0, 0).join()
        val lamp = base.add(1.0, 0.0, 0.0)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        instance.setBlock(base, Block.REDSTONE_BLOCK)

        val simulator = RedstoneRegistrations.simulator(instance)
        Assertions.assertNotNull(simulator)
        simulator!!.tick()
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun unregisteringInstanceRemovesSimulator() {
        RedstoneRegistrations.install(instance)
        Assertions.assertNotNull(RedstoneRegistrations.simulator(instance))

        MinecraftServer.getInstanceManager().unregisterInstance(instance)

        Assertions.assertNull(RedstoneRegistrations.simulator(instance))
    }
}
