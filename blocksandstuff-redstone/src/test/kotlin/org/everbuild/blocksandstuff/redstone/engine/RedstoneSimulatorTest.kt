package org.everbuild.blocksandstuff.redstone.engine

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.PowerSource
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.blocksandstuff.redstone.UpdateContext
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

    @Test
    fun installReschedulesAlreadyLoadedChunks() {
        val lamp = base.add(1.0, 0.0, 0.0)
        instance.setBlock(lamp, Block.REDSTONE_LAMP)
        instance.setBlock(base, Block.REDSTONE_BLOCK)
        RedstoneRegistrations.uninstall(instance)

        val reloaded = RedstoneRegistrations.install(instance)
        Assertions.assertEquals("false", instance.getBlock(lamp).getProperty("lit"))

        // <@AI_UNREVIEWED>
        reloaded.tick()
        reloaded.tick()
        // </<@AI_UNREVIEWED>

        Assertions.assertEquals("true", instance.getBlock(lamp).getProperty("lit"))
    }

    @Test
    fun scheduledTickAndNeighbourHooksAreCalled() {
        val sponge = base.add(0.0, 1.0, 0.0)
        val changed = base.add(1.0, 1.0, 0.0)
        val component = HookComponent()
        RedstoneRegistrations.registry.register(Block.SPONGE) { component }

        instance.setBlock(sponge, Block.SPONGE)
        simulator.tick()
        simulator.tick()
        simulator.tick()

        Assertions.assertTrue(component.scheduledTicks >= 1)

        instance.setBlock(changed, Block.STONE)
        simulator.tick()
        simulator.tick()

        Assertions.assertTrue(
            component.neighbourChanges.any { it.first == changed.asBlockVec() && it.second == BlockFace.EAST },
            "expected neighbour change from east, got ${component.neighbourChanges}",
        )
    }

    @Test
    fun chunkUnloadDropsCachedStateAndPendingTicks() {
        instance.setBlock(base.add(1.0, 0.0, 0.0), Block.REDSTONE_LAMP)
        instance.setBlock(base, Block.REDSTONE_BLOCK)
        Assertions.assertTrue(simulator.pendingTickCount > 0)
        Assertions.assertTrue(simulator.cachedComponentCount > 0)

        instance.unloadChunk(0, 0)
        simulator.tick()

        Assertions.assertEquals(0, simulator.pendingTickCount)
        Assertions.assertEquals(0, simulator.cachedComponentCount)
    }

    @Test
    fun blockChangesAtChunkBorderDoNotReadUnloadedChunks() {
        val border = Pos(15.0, 64.0, 8.0)
        instance.setBlock(border, Block.REDSTONE_BLOCK)
        Assertions.assertFalse(instance.isChunkLoaded(1, 0))

        Assertions.assertDoesNotThrow {
            simulator.onBlockChanged(border)
            simulator.tick()
        }

        Assertions.assertFalse(instance.isChunkLoaded(1, 0))
    }

    @Test
    fun nonConvergentDeltaIsCarriedIntoFollowingTick() {
        val position = base.add(0.0, 1.0, 0.0)
        RedstoneRegistrations.registry.register(Block.SPONGE) { OscillatingComponent() }
        instance.setBlock(position, Block.SPONGE)

        simulator.tick()
        Assertions.assertTrue(simulator.lastNonConvergent)
        Assertions.assertTrue(simulator.lastDeltaSteps > 0)

        simulator.tick()
        Assertions.assertTrue(simulator.lastNonConvergent)
        Assertions.assertTrue(simulator.lastDeltaSteps > 0)
    }

    @Test
    fun consumesFiltersNeighbourHooks() {
        val sponge = base.add(0.0, 1.0, 0.0)
        val west = sponge.add(-1.0, 0.0, 0.0)
        val east = sponge.add(1.0, 0.0, 0.0)
        val component = HookComponent()
        component.acceptedFaces = setOf(BlockFace.EAST)
        RedstoneRegistrations.registry.register(Block.SPONGE) { component }

        instance.setBlock(sponge, Block.SPONGE)
        simulator.tick()

        instance.setBlock(west, Block.STONE)
        simulator.tick()
        simulator.tick()
        Assertions.assertFalse(component.neighbourChanges.any { it.first == west.asBlockVec() })

        instance.setBlock(east, Block.STONE)
        simulator.tick()
        simulator.tick()
        Assertions.assertTrue(component.neighbourChanges.any { it.first == east.asBlockVec() })
    }
}

private class OscillatingComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        ctx.markDirty(ctx.position)
        return ComponentEvaluation.None
    }
}

private class HookComponent : RedstoneComponent {
    var scheduledTicks = 0
    var acceptedFaces: Set<BlockFace> = BlockFace.entries.toSet()
    val neighbourChanges = ArrayList<Pair<Point, BlockFace>>()
    private var scheduled = false

    override fun consumes(ctx: org.everbuild.blocksandstuff.redstone.ReadContext): Set<BlockFace> = acceptedFaces

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        if (!scheduled) {
            scheduled = true
            ctx.scheduleTick(2)
        }
        return ComponentEvaluation.None
    }

    override fun onScheduledTick(ctx: UpdateContext) {
        scheduledTicks++
    }

    override fun onNeighbourChanged(
        ctx: UpdateContext,
        changed: Point,
        face: BlockFace,
    ) {
        neighbourChanges.add(changed to face)
    }
}
