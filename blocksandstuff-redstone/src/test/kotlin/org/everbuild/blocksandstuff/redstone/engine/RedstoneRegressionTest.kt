package org.everbuild.blocksandstuff.redstone.engine

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.MovementDirectionSource
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.RedstoneRegistry
import org.everbuild.blocksandstuff.redstone.UpdateContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RedstoneRegressionTest {
    private lateinit var instance: InstanceContainer

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun blockChangeAtLoadedChunkEdgeDoesNotReadAnUnloadedNeighbour() {
        val simulator = RedstoneSimulator(instance, RedstoneRegistry())
        assertTrue(instance.hasEnabledAutoChunkLoad(), "exercise the default auto-loading configuration")
        assertNull(instance.getChunk(-1, 0))

        assertDoesNotThrow {
            simulator.onBlockChanged(Vec(0.0, 64.0, 8.0))
            simulator.tick()
        }
    }

    @Test
    fun sameEdgeUpdateWorksWhenTheNeighbourChunkIsLoaded() {
        instance.loadChunk(-1, 0).join()
        val simulator = RedstoneSimulator(instance, RedstoneRegistry())

        assertDoesNotThrow {
            simulator.onBlockChanged(Vec(0.0, 64.0, 8.0))
            simulator.tick()
        }
    }

    @Test
    fun finiteChunkScanLargerThanOneTickBudgetEventuallyEvaluatesEveryComponent() {
        val visited = HashSet<Point>()
        val registry = RedstoneRegistry()
        registry.register(Block.STONE) {
            object : RedstoneComponent {
                override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
                    visited.add(ctx.position)
                    return ComponentEvaluation.None
                }
            }
        }
        val simulator = RedstoneSimulator(instance, registry)
        val componentCount = 4097

        // No global redstone listener is installed: only the explicit chunk scan supplies work.
        // Components never mutate state or enqueue neighbours, so this finite workload cannot oscillate.
        repeat(componentCount) { index ->
            val x = 2 + index % 12
            val z = 2 + (index / 12) % 12
            val y = 64 + index / (12 * 12)
            instance.setBlock(x, y, z, Block.STONE, false)
        }
        simulator.onChunkLoaded(requireNotNull(instance.getChunk(0, 0)))

        repeat(10) { simulator.tick() }

        assertEquals(componentCount, visited.size, "budget overflow must defer work rather than discard it")
    }

    @Test
    fun preferredMovementFaceOrdersEffectsWithoutDroppingOtherDirections() {
        val effectOrder = mutableListOf<BlockFace>()
        val registry = RedstoneRegistry()
        registry.register(Block.REDSTONE_LAMP) { PreferredDirectionComponent() }
        registry.register(Block.STONE) { DeferredEffectComponent(BlockFace.EAST, effectOrder) }
        registry.register(Block.DIRT) { DeferredEffectComponent(BlockFace.NORTH, effectOrder) }
        val simulator = RedstoneSimulator(instance, registry)
        val preferencePosition = Vec(2.0, 64.0, 2.0)
        val eastPosition = Vec(7.0, 64.0, 7.0)
        val northPosition = Vec(12.0, 64.0, 12.0)

        instance.setBlock(preferencePosition, Block.REDSTONE_LAMP)
        instance.setBlock(eastPosition, Block.STONE)
        instance.setBlock(northPosition, Block.DIRT)
        simulator.onBlockChanged(preferencePosition)
        simulator.onBlockChanged(eastPosition)
        simulator.onBlockChanged(northPosition)

        simulator.tick()

        assertEquals(listOf(BlockFace.EAST, BlockFace.NORTH), effectOrder)
    }
}

// <@AI_UNREVIEWED>
private class PreferredDirectionComponent : RedstoneComponent, MovementDirectionSource {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation =
        if (ctx.block().getProperty("lit") == "true") {
            ComponentEvaluation.None
        } else {
            ComponentEvaluation.SetState(ctx.block().withProperty("lit", "true"))
        }

    override fun movementFace(ctx: ReadContext): BlockFace = BlockFace.EAST
}

private class DeferredEffectComponent(
    private val movementFace: BlockFace,
    private val effectOrder: MutableList<BlockFace>,
) : RedstoneComponent {
    private var triggered = false

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        if (triggered) return ComponentEvaluation.None
        triggered = true
        return ComponentEvaluation.Trigger(movementFace) { effectOrder += movementFace }
    }
}
// </<@AI_UNREVIEWED>
