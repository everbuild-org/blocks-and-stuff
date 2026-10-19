package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
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
        settle()
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
        simulator.tick()
        Assertions.assertEquals("true", instance.getBlock(base).getProperty("powered"))

        simulator.tick()
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
