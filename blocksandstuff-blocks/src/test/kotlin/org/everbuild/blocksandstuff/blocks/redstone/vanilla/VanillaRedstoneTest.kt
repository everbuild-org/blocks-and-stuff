package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.placement.RedstoneWirePlacementRule
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.blocksandstuff.redstone.engine.RedstoneSimulator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VanillaRedstoneTest {
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
        MinecraftServer.stopCleanly()
    }

    private fun settle(ticks: Int = 3) {
        repeat(ticks) { simulator.tick() }
    }

    @Test
    fun wirePropagationDecaysWithDistance() {
        instance.setBlock(base, Block.REDSTONE_BLOCK)
        instance.setBlock(base.add(1.0, 0.0, 0.0), Block.REDSTONE_WIRE)
        instance.setBlock(base.add(2.0, 0.0, 0.0), Block.REDSTONE_WIRE)
        instance.setBlock(base.add(3.0, 0.0, 0.0), Block.REDSTONE_WIRE)

        settle()

        Assertions.assertEquals("15", instance.getBlock(base.add(1.0, 0.0, 0.0)).getProperty("power"))
        Assertions.assertEquals("14", instance.getBlock(base.add(2.0, 0.0, 0.0)).getProperty("power"))
        Assertions.assertEquals("13", instance.getBlock(base.add(3.0, 0.0, 0.0)).getProperty("power"))
    }

    @Test
    fun leverPowersLamp() {
        val lamp = base.add(1.0, 0.0, 0.0)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        instance.setBlock(base, Block.LEVER.withProperty("powered", "false"))
        settle()
        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))

        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        settle()
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))

        instance.setBlock(base, Block.LEVER.withProperty("powered", "false"))
        settle(5)
        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun redstoneLampTurnsOffAfterFourTicks() {
        val lamp = base.add(1.0, 0.0, 0.0)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        settle()
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))

        instance.setBlock(base, Block.LEVER.withProperty("powered", "false"))
        simulator.tick()
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))

        repeat(3) { simulator.tick() }
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))

        simulator.tick()
        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun torchTurnsOffWhenSupportPowered() {
        val support = base
        val torch = base.add(0.0, 1.0, 0.0)
        val wire = base.add(1.0, 0.0, 0.0)
        val source = base.add(2.0, 0.0, 0.0)

        instance.setBlock(support, Block.STONE)
        instance.setBlock(torch, Block.REDSTONE_TORCH.withProperty("lit", "true"))
        instance.setBlock(wire, Block.REDSTONE_WIRE)
        settle()
        Assertions.assertEquals("true", instance.getBlock(torch).getProperty("lit"))

        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle()
        Assertions.assertEquals("false", instance.getBlock(torch).getProperty("lit"))

        instance.setBlock(source, Block.AIR)
        settle()
        Assertions.assertEquals("true", instance.getBlock(torch).getProperty("lit"))
    }

    @Test
    fun wallTorchTurnsOffWhenSupportPowered() {
        val support = base
        val torch = base.add(1.0, 0.0, 0.0)

        instance.setBlock(support, Block.STONE)
        instance.setBlock(torch, Block.REDSTONE_WALL_TORCH.withProperty("facing", "east").withProperty("lit", "true"))
        settle()
        Assertions.assertEquals("true", instance.getBlock(torch).getProperty("lit"))

        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.REDSTONE_BLOCK)
        settle()
        Assertions.assertEquals("false", instance.getBlock(torch).getProperty("lit"))

        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.AIR)
        settle()
        Assertions.assertEquals("true", instance.getBlock(torch).getProperty("lit"))
    }

    @Test
    fun redstoneTorchBurnsOutWhenToggledTooQuickly() {
        val support = base
        val torch = base.add(0.0, 1.0, 0.0)
        val source = base.add(1.0, 0.0, 0.0)

        instance.setBlock(support, Block.STONE)
        instance.setBlock(torch, Block.REDSTONE_TORCH.withProperty("lit", "true"))
        settle()

        repeat(8) {
            instance.setBlock(source, Block.REDSTONE_BLOCK)
            settle(3)
            instance.setBlock(source, Block.AIR)
            settle(3)
        }

        Assertions.assertEquals("false", instance.getBlock(torch).getProperty("lit"))
        settle(5)
        Assertions.assertEquals("false", instance.getBlock(torch).getProperty("lit"))

        settle(170)
        Assertions.assertEquals("true", instance.getBlock(torch).getProperty("lit"))
    }

    @Test
    fun torchesOnSameBlockDoNotTurnEachOtherOff() {
        val support = base
        val topTorch = base.add(0.0, 1.0, 0.0)
        val eastTorch = base.add(1.0, 0.0, 0.0)
        val southTorch = base.add(0.0, 0.0, 1.0)

        instance.setBlock(support, Block.STONE)
        instance.setBlock(topTorch, Block.REDSTONE_TORCH.withProperty("lit", "true"))
        instance.setBlock(eastTorch, Block.REDSTONE_WALL_TORCH.withProperty("facing", "east").withProperty("lit", "true"))
        instance.setBlock(southTorch, Block.REDSTONE_WALL_TORCH.withProperty("facing", "south").withProperty("lit", "true"))
        settle()

        Assertions.assertEquals("true", instance.getBlock(topTorch).getProperty("lit"))
        Assertions.assertEquals("true", instance.getBlock(eastTorch).getProperty("lit"))
        Assertions.assertEquals("true", instance.getBlock(southTorch).getProperty("lit"))

        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("false", instance.getBlock(topTorch).getProperty("lit"))
        Assertions.assertEquals("false", instance.getBlock(eastTorch).getProperty("lit"))
        Assertions.assertEquals("false", instance.getBlock(southTorch).getProperty("lit"))
    }

    @Test
    fun ironDoorOpensAndClosesWithRedstone() {
        val lower = Block.IRON_DOOR.withProperty("half", "lower")
        val upper = Block.IRON_DOOR.withProperty("half", "upper")
        val wire = base.sub(0.0, 0.0, 1.0)
        val source = base.sub(0.0, 0.0, 2.0)

        instance.setBlock(base, lower)
        instance.setBlock(base.add(0.0, 1.0, 0.0), upper)
        instance.setBlock(wire, Block.REDSTONE_WIRE)
        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("open"))
        Assertions.assertEquals("true", instance.getBlock(base.add(0.0, 1.0, 0.0)).getProperty("open"))

        instance.setBlock(source, Block.AIR)
        settle()

        Assertions.assertEquals("false", instance.getBlock(base).getProperty("open"))
        Assertions.assertEquals("false", instance.getBlock(base.add(0.0, 1.0, 0.0)).getProperty("open"))
    }

    @Test
    fun replacingWireKeepsLampLit() {
        val wire1 = base.add(1.0, 0.0, 0.0)
        val wire2 = base.add(2.0, 0.0, 0.0)
        val lamp = base.add(3.0, 0.0, 0.0)

        instance.setBlock(base, Block.REDSTONE_BLOCK)
        instance.setBlock(wire1, Block.REDSTONE_WIRE)
        instance.setBlock(wire2, Block.REDSTONE_WIRE)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        settle()
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))

        instance.setBlock(wire2, Block.REDSTONE_WIRE)
        settle()
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun repeaterDelaysThenPowersLamp() {
        val repeater = base.add(1.0, 0.0, 0.0)
        val lamp = base.add(2.0, 0.0, 0.0)

        instance.setBlock(base, Block.LEVER.withProperty("powered", "false"))
        instance.setBlock(repeater, Block.REPEATER.withProperty("facing", "west"))
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        settle()
        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))

        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        simulator.tick()
        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))

        settle(3)
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun repeaterIsPoweredByWeaklyPoweredBlock() {
        val repeater = base
        val stone = base.sub(1.0, 0.0, 0.0)
        val dust = stone.add(0.0, 1.0, 0.0)
        val source = dust.sub(1.0, 0.0, 0.0)
        val lamp = base.add(1.0, 0.0, 0.0)

        instance.setBlock(repeater, Block.REPEATER.withProperty("facing", "west"))
        instance.setBlock(stone, Block.STONE)
        instance.setBlock(dust, Block.REDSTONE_WIRE)
        instance.setBlock(source, Block.REDSTONE_BLOCK)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        settle(5)

        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun parallelRepeatersDoNotLockEachOther() {
        val second = base.add(0.0, 0.0, 1.0)
        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.LEVER.withProperty("powered", "true"))
        instance.setBlock(second.sub(1.0, 0.0, 0.0), Block.LEVER.withProperty("powered", "true"))
        instance.setBlock(base, Block.REPEATER.withProperty("facing", "west"))
        instance.setBlock(second, Block.REPEATER.withProperty("facing", "west"))

        settle(6)

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("powered"))
        Assertions.assertEquals("true", instance.getBlock(second).getProperty("powered"))
    }

    @Test
    fun comparatorPassesRearSignal() {
        val comparator = base.add(1.0, 0.0, 0.0)
        val lamp = base.add(2.0, 0.0, 0.0)

        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        instance.setBlock(comparator, Block.COMPARATOR.withProperty("facing", "west").withProperty("mode", "compare"))
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        settle()

        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun comparatorSubtractCancelsEqualInputs() {
        val comparator = base.add(1.0, 0.0, 0.0)
        val lamp = base.add(2.0, 0.0, 0.0)

        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        instance.setBlock(comparator.sub(0.0, 0.0, 1.0), Block.LEVER.withProperty("powered", "true"))
        instance.setBlock(comparator, Block.COMPARATOR.withProperty("facing", "west").withProperty("mode", "subtract"))
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        settle()

        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun copperBulbTogglesLitOnRisingEdge() {
        val bulb = base.add(1.0, 0.0, 0.0)
        instance.setBlock(base, Block.LEVER.withProperty("powered", "false"))
        instance.setBlock(bulb, Block.COPPER_BULB)
        settle()

        Assertions.assertEquals("false", instance.getBlock(bulb).getProperty("lit"))
        Assertions.assertEquals("false", instance.getBlock(bulb).getProperty("powered"))

        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        settle()
        Assertions.assertEquals("true", instance.getBlock(bulb).getProperty("powered"))
        Assertions.assertEquals("true", instance.getBlock(bulb).getProperty("lit"))

        settle(5)
        Assertions.assertEquals("true", instance.getBlock(bulb).getProperty("lit"))

        instance.setBlock(base, Block.LEVER.withProperty("powered", "false"))
        settle()
        Assertions.assertEquals("false", instance.getBlock(bulb).getProperty("powered"))
        Assertions.assertEquals("true", instance.getBlock(bulb).getProperty("lit"))

        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        settle()
        Assertions.assertEquals("true", instance.getBlock(bulb).getProperty("powered"))
        Assertions.assertEquals("false", instance.getBlock(bulb).getProperty("lit"))
    }

    @Test
    fun targetStoresProjectilePowerAndDecays() {
        instance.setBlock(base, Block.TARGET)
        settle()

        TargetBlockRegistration.hit(instance, base, 11, 8)
        Assertions.assertEquals("11", instance.getBlock(base).getProperty("power"))
        Assertions.assertEquals(11, simulator.weakSignal(base, BlockFace.TOP))

        settle(8)
        Assertions.assertEquals("0", instance.getBlock(base).getProperty("power"))
    }

    @Test
    fun lightningRodPowersAndDecays() {
        instance.setBlock(base, Block.LIGHTNING_ROD.withProperty("facing", "up").withProperty("powered", "false"))
        settle()

        LightningRodBlockRegistration.strike(instance, base)
        settle()
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("powered"))
        Assertions.assertEquals(15, simulator.weakSignal(base, BlockFace.TOP))
        Assertions.assertEquals(15, simulator.directSignal(base, BlockFace.BOTTOM))

        settle(8)
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("powered"))
    }

    @Test
    fun comparatorDelaysByOneRedstoneTick() {
        val comparator = base.add(1.0, 0.0, 0.0)
        val lamp = base.add(2.0, 0.0, 0.0)
        instance.setBlock(base, Block.LEVER.withProperty("powered", "false"))
        instance.setBlock(comparator, Block.COMPARATOR.withProperty("facing", "west").withProperty("mode", "compare"))
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        settle()
        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))

        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        simulator.tick()
        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))

        settle(2)
        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun comparatorClockDoesNotSpamDeltaCycle() {
        val output = base.add(1.0, 0.0, 0.0)
        val corner = base.add(1.0, 0.0, 1.0)
        val side = base.add(0.0, 0.0, 1.0)

        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.REDSTONE_BLOCK)
        instance.setBlock(base, Block.COMPARATOR.withProperty("facing", "west").withProperty("mode", "subtract"))
        instance.setBlock(output, Block.REDSTONE_WIRE)
        instance.setBlock(corner, Block.REDSTONE_WIRE)
        instance.setBlock(side, Block.REDSTONE_WIRE)
        settle(8)

        val values = HashSet<String>()
        repeat(20) {
            simulator.tick()
            values.add(instance.getBlock(output).getProperty("power") ?: "0")
            Assertions.assertTrue(simulator.lastDeltaSteps < 256, "delta steps: ${simulator.lastDeltaSteps}")
        }
        Assertions.assertTrue(values.size > 1, "comparator clock did not oscillate")
    }

    @Test
    fun comparatorSubtractionClockWithConductingBlock() {
        val outputBlock = base.add(1.0, 0.0, 0.0)
        val outputDust = base.add(1.0, 0.0, 1.0)
        val sideDust = base.add(0.0, 0.0, 1.0)

        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.REDSTONE_BLOCK)
        instance.setBlock(base, Block.COMPARATOR.withProperty("facing", "west").withProperty("mode", "subtract"))
        instance.setBlock(outputBlock, Block.STONE)
        instance.setBlock(outputDust, Block.REDSTONE_WIRE)
        instance.setBlock(sideDust, Block.REDSTONE_WIRE)
        settle(8)

        val outputValues = HashSet<String>()
        val sideValues = HashSet<String>()
        repeat(20) {
            simulator.tick()
            outputValues.add(instance.getBlock(outputDust).getProperty("power") ?: "0")
            sideValues.add(instance.getBlock(sideDust).getProperty("power") ?: "0")
            Assertions.assertTrue(simulator.lastDeltaSteps < 256, "delta steps: ${simulator.lastDeltaSteps}")
        }
        Assertions.assertTrue(outputValues.size > 1, "comparator clock did not oscillate")
        Assertions.assertTrue(sideValues.size > 1, "comparator side input did not oscillate")
    }

    @Test
    fun repeaterStretchesShortPulses() {
        val repeater = base.add(1.0, 0.0, 0.0)
        instance.setBlock(base, Block.LEVER.withProperty("powered", "false"))
        instance.setBlock(repeater, Block.REPEATER.withProperty("facing", "west"))
        settle()

        instance.setBlock(base, Block.LEVER.withProperty("powered", "true"))
        simulator.tick()
        instance.setBlock(base, Block.LEVER.withProperty("powered", "false"))
        simulator.tick()
        simulator.tick()
        Assertions.assertEquals("true", instance.getBlock(repeater).getProperty("powered"))

        settle(2)
        Assertions.assertEquals("false", instance.getBlock(repeater).getProperty("powered"))
    }

    @Test
    fun repeaterWritesLockedStateAndFreezes() {
        val target = base
        val rear = base.sub(1.0, 0.0, 0.0)
        val side = base.add(0.0, 0.0, -1.0)
        val sideSource = base.add(0.0, 0.0, -2.0)

        instance.setBlock(rear, Block.LEVER.withProperty("powered", "false"))
        instance.setBlock(target, Block.REPEATER.withProperty("facing", "west"))
        instance.setBlock(side, Block.REPEATER.withProperty("facing", "north"))
        instance.setBlock(sideSource, Block.REDSTONE_BLOCK)
        settle(6)

        Assertions.assertEquals("true", instance.getBlock(target).getProperty("locked"))
        Assertions.assertEquals("false", instance.getBlock(target).getProperty("powered"))

        instance.setBlock(rear, Block.LEVER.withProperty("powered", "true"))
        settle(4)
        Assertions.assertEquals("false", instance.getBlock(target).getProperty("powered"))

        instance.setBlock(sideSource, Block.AIR)
        settle(8)
        Assertions.assertEquals("false", instance.getBlock(target).getProperty("locked"))
        Assertions.assertEquals("true", instance.getBlock(target).getProperty("powered"))
    }

    @Test
    fun daylightDetectorTracksTimeOfDay() {
        instance.setTime(6000)
        instance.setBlock(base, Block.DAYLIGHT_DETECTOR)
        settle()
        Assertions.assertEquals("15", instance.getBlock(base).getProperty("power"))

        instance.setTime(18000)
        settle(21)
        Assertions.assertEquals("0", instance.getBlock(base).getProperty("power"))
    }

    @Test
    fun invertedDaylightDetectorReturnsNightSkyLight() {
        instance.setTime(18000)
        instance.setBlock(base, Block.DAYLIGHT_DETECTOR.withProperty("inverted", "true"))
        settle()
        Assertions.assertEquals("11", instance.getBlock(base).getProperty("power"))
    }

    @Test
    fun daylightDetectorOnSolidBlockSettles() {
        instance.setTime(6000)
        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(base, Block.DAYLIGHT_DETECTOR)
        settle()
        Assertions.assertEquals("15", instance.getBlock(base).getProperty("power"))
    }

    @Test
    fun pistonExtendsAndRetracts() {
        val front = base.add(1.0, 0.0, 0.0)
        val pushed = base.add(2.0, 0.0, 0.0)
        val source = base.sub(1.0, 0.0, 0.0)

        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        instance.setBlock(front, Block.STONE)
        settle()

        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle()
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(front).key())
        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(pushed).key())

        instance.setBlock(source, Block.AIR)
        settle()
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(front).key())
        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(pushed).key())
    }

    @Test
    fun pistonExtendsUpward() {
        val front = base.add(0.0, 1.0, 0.0)
        val pushed = base.add(0.0, 2.0, 0.0)
        val source = base.add(1.0, 0.0, 0.0)

        instance.setBlock(base, Block.PISTON.withProperty("facing", "up"))
        instance.setBlock(front, Block.STONE)
        settle()

        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(front).key())
        Assertions.assertEquals("up", instance.getBlock(front).getProperty("facing"))
        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(pushed).key())
    }

    @Test
    fun pistonPoweredFromAbove() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        settle()
        instance.setBlock(base.add(0.0, 1.0, 0.0), Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun pistonQuasiPoweredFromAboveFrontAfterUpdate() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        settle()
        instance.setBlock(base.add(1.0, 1.0, 0.0), Block.REDSTONE_BLOCK)
        settle()
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))

        instance.setBlock(base.add(1.0, 0.0, 0.0), Block.STONE)
        settle()
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun upwardPistonQuasiPoweredFromTwoAboveAfterUpdate() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "up"))
        settle()
        instance.setBlock(base.add(0.0, 2.0, 0.0), Block.REDSTONE_BLOCK)
        settle()
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))

        instance.setBlock(base.add(0.0, 0.0, 1.0), Block.STONE)
        settle()
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun extendedUpwardPistonStaysExtendedWithRedstoneBlockOnHead() {
        val source = base.sub(1.0, 0.0, 0.0)
        val head = base.add(0.0, 1.0, 0.0)
        val onHead = base.add(0.0, 2.0, 0.0)

        instance.setBlock(base, Block.STICKY_PISTON.withProperty("facing", "up"))
        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle(3)
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(head).key())

        instance.setBlock(onHead, Block.REDSTONE_BLOCK)
        instance.setBlock(source, Block.AIR)
        settle(6)

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(head).key())
        Assertions.assertEquals(Block.REDSTONE_BLOCK.key(), instance.getBlock(onHead).key())
    }

    @Test
    fun pistonPushesRedstoneBlockOnBudUpdateAndHoldsIt() {
        val front = base.add(0.0, 1.0, 0.0)
        val pushed = base.add(0.0, 2.0, 0.0)

        instance.setBlock(base, Block.PISTON.withProperty("facing", "up"))
        settle()
        setBlockSilently(instance, front, Block.REDSTONE_BLOCK)

        instance.setBlock(base.add(0.0, 0.0, 1.0), Block.STONE)
        settle(4)

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(front).key())
        Assertions.assertEquals(Block.REDSTONE_BLOCK.key(), instance.getBlock(pushed).key())

        settle(10)
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.REDSTONE_BLOCK.key(), instance.getBlock(pushed).key())
    }

    @Test
    fun upwardPistonPoweredFromHeadDirectionByQc() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "up"))
        settle()
        instance.setBlock(base.add(0.0, 1.0, 0.0), Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(base.add(0.0, 1.0, 0.0)).key())
        Assertions.assertEquals(Block.REDSTONE_BLOCK.key(), instance.getBlock(base.add(0.0, 2.0, 0.0)).key())
    }

    @Test
    fun upwardPistonPoweredFromBelow() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "up"))
        settle()
        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun pistonPoweredThroughStronglyPoweredBlockBehind() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        val support = base.sub(1.0, 0.0, 0.0)
        instance.setBlock(support, Block.STONE)
        instance.setBlock(
            support.add(0.0, 1.0, 0.0),
            Block.LEVER
                .withProperty("face", "floor")
                .withProperty("facing", "north")
                .withProperty("powered", "true"),
        )
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun pistonExtendsAfterImmovableFrontRemoved() {
        val front = base.add(1.0, 0.0, 0.0)
        val qcSource = base.add(1.0, 1.0, 0.0)

        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        instance.setBlock(front, Block.OBSIDIAN)
        instance.setBlock(qcSource, Block.REDSTONE_BLOCK)
        instance.setBlock(base.add(0.0, 0.0, 1.0), Block.STONE)
        settle()
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))

        instance.setBlock(front, Block.AIR)
        settle()
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun pistonDoesNotMoveMoreThanTwelveBlocks() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        for (index in 1..13) {
            instance.setBlock(base.add(index.toDouble(), 0.0, 0.0), Block.STONE)
        }
        settle()

        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.REDSTONE_BLOCK)
        settle(5)

        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))
        for (index in 1..13) {
            Assertions.assertEquals(Block.STONE.key(), instance.getBlock(base.add(index.toDouble(), 0.0, 0.0)).key())
        }
    }

    @Test
    fun pistonPushesRedstoneBlockOnBudUpdateFromBelow() {
        val front = base.add(0.0, 1.0, 0.0)
        val pushed = base.add(0.0, 2.0, 0.0)

        instance.setBlock(base, Block.PISTON.withProperty("facing", "up"))
        settle()
        setBlockSilently(instance, front, Block.REDSTONE_BLOCK)

        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.STONE)
        settle(4)

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(front).key())
        Assertions.assertEquals(Block.REDSTONE_BLOCK.key(), instance.getBlock(pushed).key())

        settle(10)
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun pistonQuasiPoweredAfterUpdateFromBelow() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        settle()
        instance.setBlock(base.add(1.0, 1.0, 0.0), Block.REDSTONE_BLOCK)
        settle()
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))

        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.STONE)
        settle()
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun pistonPoweredByLeverThroughBlockBelow() {
        val below = base.sub(0.0, 1.0, 0.0)
        instance.setBlock(base, Block.PISTON.withProperty("facing", "up"))
        instance.setBlock(below, Block.STONE)
        instance.setBlock(
            below.add(0.0, 0.0, -1.0),
            Block.LEVER
                .withProperty("face", "wall")
                .withProperty("facing", "north")
                .withProperty("powered", "true"),
        )
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun horizontalPistonPoweredFromBelow() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        settle()
        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun upwardPistonPoweredByTorchBelow() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "up"))
        settle()
        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.REDSTONE_TORCH.withProperty("lit", "true"))
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun upwardPistonPoweredByStronglyPoweredBlockBelow() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "up"))
        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(base.sub(0.0, 2.0, 0.0), Block.REDSTONE_TORCH.withProperty("lit", "true"))
        settle()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun pistonCannotPushDropper() {
        val front = base.add(1.0, 0.0, 0.0)
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        instance.setBlock(front, Block.DROPPER.withProperty("facing", "up"))
        settle()

        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.REDSTONE_BLOCK)
        settle(5)

        Assertions.assertNotNull(Block.DROPPER.blockEntityType())
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.DROPPER.key(), instance.getBlock(front).key())
    }

    @Test
    fun pistonIgnoresPowerInFront() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        settle()
        instance.setBlock(base.add(1.0, 0.0, 0.0), Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun pistonUsesQuasiConnectivityAfterBlockUpdate() {
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        settle()

        val quasiSource = base.add(0.0, 1.0, 1.0)
        instance.setBlock(quasiSource, Block.REDSTONE_BLOCK)
        settle()
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))

        instance.setBlock(base.add(0.0, 0.0, 1.0), Block.STONE)
        settle()
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun pistonUsesMovingPistonState() {
        val front = base.add(1.0, 0.0, 0.0)
        val destination = base.add(2.0, 0.0, 0.0)
        val source = base.sub(1.0, 0.0, 0.0)

        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        instance.setBlock(front, Block.STONE)
        settle()

        instance.setBlock(source, Block.REDSTONE_BLOCK)
        simulator.tick()

        Assertions.assertEquals("true", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.MOVING_PISTON.key(), instance.getBlock(front).key())
        Assertions.assertEquals(Block.MOVING_PISTON.key(), instance.getBlock(destination).key())
        Assertions.assertNotNull(instance.getBlock(front).nbt())
        Assertions.assertNotNull(Block.MOVING_PISTON.blockEntityType())

        settle(2)
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(front).key())
        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(destination).key())
    }

    @Test
    fun pistonPushesMultipleBlocks() {
        val source = base.sub(1.0, 0.0, 0.0)
        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        instance.setBlock(base.add(1.0, 0.0, 0.0), Block.STONE)
        instance.setBlock(base.add(2.0, 0.0, 0.0), Block.DIRT)
        settle()

        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle(3)

        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(base.add(2.0, 0.0, 0.0)).key())
        Assertions.assertEquals(Block.DIRT.key(), instance.getBlock(base.add(3.0, 0.0, 0.0)).key())
    }

    @Test
    fun pistonPushesSlimeWithAttachedBlock() {
        val slime = base.add(1.0, 0.0, 0.0)
        val attached = base.add(1.0, 1.0, 0.0)
        val source = base.sub(1.0, 0.0, 0.0)

        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        instance.setBlock(slime, Block.SLIME_BLOCK)
        instance.setBlock(attached, Block.STONE)
        settle()

        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle(3)

        Assertions.assertEquals(Block.SLIME_BLOCK.key(), instance.getBlock(base.add(2.0, 0.0, 0.0)).key())
        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(base.add(2.0, 1.0, 0.0)).key())
    }

    @Test
    fun stickyPistonDropsBlockOnShortPulse() {
        val front = base.add(1.0, 0.0, 0.0)
        val dropped = base.add(2.0, 0.0, 0.0)
        val source = base.sub(1.0, 0.0, 0.0)

        instance.setBlock(base, Block.STICKY_PISTON.withProperty("facing", "east"))
        instance.setBlock(front, Block.STONE)
        settle()

        instance.setBlock(source, Block.REDSTONE_BLOCK)
        simulator.tick()
        instance.setBlock(source, Block.AIR)
        simulator.tick()

        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(dropped).key())
        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(front).key())

        settle(3)
        Assertions.assertEquals(Block.STICKY_PISTON.key(), instance.getBlock(base).key())
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))
        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(dropped).key())
        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(front).key())
    }

    @Test
    fun stickyPistonDropsFirstPushedBlockOnly() {
        val front = base.add(1.0, 0.0, 0.0)
        val source = base.sub(1.0, 0.0, 0.0)

        instance.setBlock(base, Block.STICKY_PISTON.withProperty("facing", "east"))
        instance.setBlock(front, Block.STONE)
        instance.setBlock(base.add(2.0, 0.0, 0.0), Block.DIRT)
        settle()

        instance.setBlock(source, Block.REDSTONE_BLOCK)
        simulator.tick()
        instance.setBlock(source, Block.AIR)
        simulator.tick()
        settle(3)

        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(base.add(2.0, 0.0, 0.0)).key())
        Assertions.assertEquals(Block.DIRT.key(), instance.getBlock(base.add(3.0, 0.0, 0.0)).key())
        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(front).key())
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun observerPulseDropsStickyPistonBlock() {
        val front = base.add(1.0, 0.0, 0.0)
        val dropped = base.add(2.0, 0.0, 0.0)
        val observer = base.sub(1.0, 0.0, 0.0)
        val watched = base.sub(2.0, 0.0, 0.0)

        instance.setBlock(base, Block.STICKY_PISTON.withProperty("facing", "east"))
        instance.setBlock(front, Block.STONE)
        instance.setBlock(observer, Block.OBSERVER.withProperty("facing", "west"))
        settle()

        instance.setBlock(watched, Block.STONE)
        settle(10)

        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(dropped).key())
        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(front).key())
        Assertions.assertEquals(Block.STICKY_PISTON.key(), instance.getBlock(base).key())
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("extended"))
    }

    @Test
    fun breakingExtendedPistonRemovesHead() {
        val front = base.add(1.0, 0.0, 0.0)
        val source = base.sub(1.0, 0.0, 0.0)

        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        instance.setBlock(front, Block.STONE)
        settle()
        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle(3)
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(front).key())

        instance.setBlock(base, Block.AIR)
        settle()
        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(front).key())
    }

    @Test
    fun breakingExtendedPistonHeadRemovesBase() {
        val front = base.add(1.0, 0.0, 0.0)
        val source = base.sub(1.0, 0.0, 0.0)

        instance.setBlock(base, Block.PISTON.withProperty("facing", "east"))
        settle()
        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle(3)
        Assertions.assertEquals(Block.PISTON_HEAD.key(), instance.getBlock(front).key())

        instance.setBlock(front, Block.AIR)
        settle()
        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(base).key())
    }

    @Test
    fun stickyPistonPullsBlockBack() {
        val front = base.add(1.0, 0.0, 0.0)
        val source = base.sub(1.0, 0.0, 0.0)

        instance.setBlock(base, Block.STICKY_PISTON.withProperty("facing", "east"))
        instance.setBlock(front, Block.STONE)
        settle()

        instance.setBlock(source, Block.REDSTONE_BLOCK)
        settle()
        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(base.add(2.0, 0.0, 0.0)).key())

        instance.setBlock(source, Block.AIR)
        settle()
        Assertions.assertEquals(Block.STONE.key(), instance.getBlock(front).key())
    }

    @Test
    fun observerPulsesOnWatchedChange() {
        val watched = base.add(1.0, 0.0, 0.0)
        instance.setBlock(base, Block.OBSERVER.withProperty("facing", "east"))
        settle()

        instance.setBlock(watched, Block.STONE)
        settle(3)
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("powered"))

        settle(2)
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("powered"))
    }

    @Test
    fun dispenserTriggersWhenPowered() {
        instance.setBlock(base, Block.DISPENSER.withProperty("facing", "east"))
        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.LEVER.withProperty("powered", "false"))
        settle()
        Assertions.assertEquals("false", instance.getBlock(base).getProperty("triggered"))

        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.LEVER.withProperty("powered", "true"))
        settle()
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("triggered"))
    }

    @Test
    fun tntIgnitesWhenPowered() {
        instance.setBlock(base, Block.TNT)
        instance.setBlock(base.sub(1.0, 0.0, 0.0), Block.LEVER.withProperty("powered", "true"))
        settle()

        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(base).key())
    }

    @Test
    fun adjacentWiresConnectAndSurviveEngineTick() {
        val rule = RedstoneWirePlacementRule(Block.REDSTONE_WIRE)
        val second = base.add(1.0, 0.0, 0.0)
        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(second.sub(0.0, 1.0, 0.0), Block.STONE)

        instance.setBlock(base, place(rule, base))
        instance.setBlock(second, place(rule, second))
        instance.setBlock(base, rule.blockUpdate(BlockPlacementRule.UpdateState(instance, base, instance.getBlock(base), BlockFace.TOP)))
        settle()

        Assertions.assertEquals("side", instance.getBlock(base).getProperty("east"))
        Assertions.assertEquals("side", instance.getBlock(second).getProperty("west"))
    }

    private fun place(
        rule: RedstoneWirePlacementRule,
        pos: Pos,
    ): Block =
        requireNotNull(
            rule.blockPlace(
                BlockPlacementRule.PlacementState(instance, Block.REDSTONE_WIRE, BlockFace.TOP, pos, null, null, null, false),
            ),
        )

    @Test
    fun staircaseWireConnectsViaEngine() {
        val upper = base.add(1.0, 1.0, 0.0)
        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(upper.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(base, Block.REDSTONE_WIRE)
        instance.setBlock(upper, Block.REDSTONE_WIRE)
        settle()

        Assertions.assertEquals("up", instance.getBlock(base).getProperty("east"))
        Assertions.assertEquals("side", instance.getBlock(upper).getProperty("west"))
    }

    @Test
    fun conductiveBlockCutsVerticalWirePower() {
        val between = base.add(0.0, 1.0, 0.0)
        val upper = base.add(1.0, 1.0, 0.0)
        instance.setBlock(base.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(upper.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(between, Block.STONE)
        instance.setBlock(base, Block.REDSTONE_WIRE)
        instance.setBlock(upper, Block.REDSTONE_WIRE)
        instance.setBlock(upper.add(1.0, 0.0, 0.0), Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("0", instance.getBlock(base).getProperty("power"))

        instance.setBlock(between, Block.GLASS)
        settle()

        Assertions.assertEquals("14", instance.getBlock(base).getProperty("power"))
    }

    @Test
    fun staircaseWireReceivesPowerFromUpperWire() {
        val upper = base.add(1.0, 0.0, 0.0)
        val lower = base.add(1.0, -1.0, 1.0)
        instance.setBlock(upper.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(lower.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(base, Block.REDSTONE_BLOCK)
        instance.setBlock(upper, Block.REDSTONE_WIRE)
        settle()
        Assertions.assertEquals("15", instance.getBlock(upper).getProperty("power"))

        instance.setBlock(lower, Block.REDSTONE_WIRE)
        settle()

        Assertions.assertEquals("14", instance.getBlock(lower).getProperty("power"))
    }

    @Test
    fun staircaseWireReceivesPowerFromLowerWire() {
        val lower = base.add(1.0, 0.0, 0.0)
        val upper = base.add(1.0, 1.0, 1.0)
        instance.setBlock(lower.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(upper.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(base, Block.REDSTONE_BLOCK)
        instance.setBlock(lower, Block.REDSTONE_WIRE)
        instance.setBlock(upper, Block.REDSTONE_WIRE)
        settle()

        Assertions.assertEquals("15", instance.getBlock(lower).getProperty("power"))
        Assertions.assertEquals("14", instance.getBlock(upper).getProperty("power"))
    }

    @Test
    fun removingStaircaseWireClearsLowerPower() {
        val upper = base.add(1.0, 0.0, 0.0)
        val lower = base.add(1.0, -1.0, 1.0)
        instance.setBlock(upper.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(lower.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(base, Block.REDSTONE_BLOCK)
        instance.setBlock(upper, Block.REDSTONE_WIRE)
        instance.setBlock(lower, Block.REDSTONE_WIRE)
        settle()
        Assertions.assertEquals("14", instance.getBlock(lower).getProperty("power"))

        instance.setBlock(upper, Block.AIR)
        settle()

        Assertions.assertEquals("0", instance.getBlock(lower).getProperty("power"))
    }

    @Test
    fun staircasePowerPropagatesWhenSourceChanges() {
        val upper = base.add(1.0, 0.0, 0.0)
        val lower = base.add(1.0, -1.0, 1.0)
        instance.setBlock(upper.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(lower.sub(0.0, 1.0, 0.0), Block.STONE)
        instance.setBlock(upper, Block.REDSTONE_WIRE)
        instance.setBlock(lower, Block.REDSTONE_WIRE)
        settle()
        Assertions.assertEquals("0", instance.getBlock(lower).getProperty("power"))

        instance.setBlock(base, Block.REDSTONE_BLOCK)
        settle()

        Assertions.assertEquals("15", instance.getBlock(upper).getProperty("power"))
        Assertions.assertEquals("14", instance.getBlock(lower).getProperty("power"))
    }

    @Test
    fun manuallyOpenedTrapdoorStaysOpen() {
        val trapdoor = base.add(1.0, 0.0, 0.0)
        instance.setBlock(trapdoor, Block.OAK_TRAPDOOR.withProperty("open", "true"))
        settle()

        instance.setBlock(base, Block.STONE)
        instance.setBlock(base, Block.AIR)
        settle()

        Assertions.assertEquals("true", instance.getBlock(trapdoor).getProperty("open"))
    }
}
