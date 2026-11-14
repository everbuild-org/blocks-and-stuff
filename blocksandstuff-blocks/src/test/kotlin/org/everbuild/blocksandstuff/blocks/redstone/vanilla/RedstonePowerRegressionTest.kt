package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.RedstoneRegistry
import org.everbuild.blocksandstuff.redstone.engine.RedstoneSimulator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RedstonePowerRegressionTest {
    private lateinit var instance: InstanceContainer
    private lateinit var simulator: RedstoneSimulator
    private val support = Vec(8.0, 64.0, 8.0)
    private val lever = support.sub(1.0, 0.0, 0.0)
    private val wire = support.add(1.0, 0.0, 0.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        val registry = RedstoneRegistry()
        registry.registerVanillaComponents()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        simulator = RedstoneSimulator(instance, registry)
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun placeCircuit(supportBlock: Block) {
        assertTrue(supportBlock.redstoneConductor())
        instance.setBlock(support, supportBlock, false)
        instance.setBlock(wire.sub(0.0, 1.0, 0.0), Block.STONE, false)
        instance.setBlock(wire, Block.REDSTONE_WIRE, false)
        instance.setBlock(
            lever,
            Block.LEVER.withProperty("face", "wall").withProperty("facing", "west").withProperty("powered", "true"),
            false,
        )
        assertEquals(15, simulator.directSignal(lever, BlockFace.EAST), "lever must strongly power its support")
        // Explicitly wake the wire too, isolating conduction from the separate neighbour-invalidation bug.
        simulator.onBlockChanged(lever)
        simulator.onBlockChanged(wire)
        repeat(6) { simulator.tick() }
    }

    @Test
    fun stronglyPoweredPlainStoneFeedsDust() {
        placeCircuit(Block.STONE)

        assertEquals(15, simulator.weakSignal(support, BlockFace.EAST))
        assertEquals("15", instance.getBlock(wire).getProperty("power"))
    }

    @Test
    fun stronglyPoweredLampMustAlsoFeedDust() {
        placeCircuit(Block.REDSTONE_LAMP)

        assertAll(
            { assertEquals("true", instance.getBlock(support).getProperty("lit"), "the source activates the lamp") },
            { assertEquals(15, simulator.weakSignal(support, BlockFace.EAST), "a registered component can also conduct") },
            { assertEquals("15", instance.getBlock(wire).getProperty("power"), "dust must receive conducted strong power") },
        )
    }

    @Test
    fun stickyBranchMustNotIncludeAnImmovableBlockOutsideItsStructure() {
        val piston = Vec(8.0, 64.0, 8.0)
        val sentinel = piston.add(-1.0, 1.0, 0.0)
        instance.setBlock(piston, Block.PISTON.withProperty("facing", "east"), false)
        instance.setBlock(piston.add(1.0, 0.0, 0.0), Block.SLIME_BLOCK, false)
        instance.setBlock(piston.add(1.0, 1.0, 0.0), Block.SLIME_BLOCK, false)
        instance.setBlock(piston.add(0.0, 1.0, 0.0), Block.STONE, false)
        instance.setBlock(sentinel, Block.OBSIDIAN, false)

        val resolver = PistonStructureResolver(instance, piston, BlockFace.EAST, true)

        assertTrue(resolver.resolve(), "the stone separates the immovable sentinel from sticky adhesion")
        assertEquals(
            setOf(piston.add(1.0, 0.0, 0.0), piston.add(1.0, 1.0, 0.0), piston.add(0.0, 1.0, 0.0)),
            resolver.toPush.toSet(),
            "only the two slime blocks and their attached stone belong in the movement list",
        )
    }
}
