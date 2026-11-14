package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.blocksandstuff.redstone.VibrationBus
import org.everbuild.blocksandstuff.redstone.engine.RedstoneSimulator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SculkShriekerTest {
    private lateinit var instance: Instance
    private lateinit var simulator: RedstoneSimulator
    private val base = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        RedstoneRegistrations.enable()
        RedstoneRegistrations.registry.registerVanillaComponents()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        (instance as InstanceContainer).setChunkSupplier(::LightingChunk)
        instance.loadChunk(0, 0).join()
        simulator = RedstoneRegistrations.install(instance)
    }

    @AfterEach
    fun teardown() {
        RedstoneRegistrations.uninstall(instance)
        VibrationBus.clear(instance)
        MinecraftServer.stopCleanly()
    }

    private fun settle(ticks: Int = 2) {
        repeat(ticks) { simulator.tick() }
    }

    @Test
    fun vibrationMakesShriekerShriekAndReset() {
        instance.setBlock(base, Block.SCULK_SHRIEKER)
        settle()

        VibrationBus.emit(instance, base.add(2.0, 0.0, 0.0), 1)
        settle(3)
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("shrieking"))

        settle(95)
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("shrieking"))

        settle(205)
        VibrationBus.emit(instance, base.add(2.0, 0.0, 0.0), 1)
        settle(3)
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("shrieking"))
    }
}