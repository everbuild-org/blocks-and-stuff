package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.blocksandstuff.redstone.VibrationBus
import org.everbuild.blocksandstuff.redstone.engine.RedstoneSimulator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SculkSensorTest {
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
    fun vibrationPowersSensorAndResets() {
        instance.setBlock(base, Block.SCULK_SENSOR)
        settle(12)

        VibrationBus.emit(instance, base.add(2.0, 0.0, 0.0), 10)
        settle(4)

        Assertions.assertEquals("12", instance.getBlock(base).getProperty("power"))

        settle(45)
        Assertions.assertEquals("0", instance.getBlock(base).getProperty("power"))
    }

    @Test
    fun outOfRangeVibrationDoesNotPowerSensor() {
        instance.setBlock(base, Block.SCULK_SENSOR)
        settle(12)

        VibrationBus.emit(instance, base.add(9.0, 0.0, 0.0), 10)
        settle(4)

        Assertions.assertEquals("0", instance.getBlock(base).getProperty("power"))
    }

    @Test
    fun calibratedSensorOnlyReactsToTunedFrequency() {
        instance.setBlock(base, Block.CALIBRATED_SCULK_SENSOR.withProperty("facing", "north"))
        instance.setBlock(base.add(0.0, 0.0, -1.0), Block.REDSTONE_BLOCK)
        settle(12)

        VibrationBus.emit(instance, base.add(2.0, 0.0, 0.0), 10)
        settle(4)
        Assertions.assertEquals("0", instance.getBlock(base).getProperty("power"))

        VibrationBus.emit(instance, base.add(2.0, 0.0, 0.0), 15)
        settle(4)
        Assertions.assertEquals("14", instance.getBlock(base).getProperty("power"))
    }

    @Test
    fun sensorPowersAdjacentRedstoneWire() {
        val wire = base.add(1.0, 0.0, 0.0)
        val lamp = base.add(2.0, 0.0, 0.0)
        instance.setBlock(base, Block.SCULK_SENSOR)
        instance.setBlock(wire, Block.REDSTONE_WIRE)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        settle(12)

        VibrationBus.emit(instance, base, 1)
        settle(3)

        Assertions.assertEquals("15", instance.getBlock(wire).getProperty("power"))
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun noteBlockEmitsVibration() {
        val note = base.add(2.0, 0.0, 0.0)
        instance.setBlock(base, Block.SCULK_SENSOR)
        instance.setBlock(note, Block.NOTE_BLOCK)
        settle(12)

        NoteBlockSound.play(instance, note, instance.getBlock(note))
        settle(4)

        Assertions.assertEquals("12", instance.getBlock(base).getProperty("power"))
    }
}
