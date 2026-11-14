package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SpongeRuleTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)

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
    fun absorbsConnectedWaterAndBecomesWet() {
        instance.setBlock(position, Block.SPONGE)
        val water = position.relative(BlockFace.TOP).asBlockVec()
        instance.setBlock(water, Block.WATER)

        val absorbed = absorbWater(instance, position.asBlockVec())

        Assertions.assertTrue(absorbed)
        Assertions.assertTrue(instance.getBlock(position).compare(Block.WET_SPONGE))
        Assertions.assertTrue(instance.getBlock(water).compare(Block.AIR))
    }

    @Test
    fun doesNothingWithoutWater() {
        instance.setBlock(position, Block.SPONGE)

        val absorbed = absorbWater(instance, position.asBlockVec())

        Assertions.assertFalse(absorbed)
        Assertions.assertTrue(instance.getBlock(position).compare(Block.SPONGE))
    }

    @Test
    fun tickRuleAbsorbsAdjacentWater() {
        instance.setBlock(position, Block.SPONGE)
        val water = position.relative(BlockFace.NORTH).asBlockVec()
        instance.setBlock(water, Block.WATER)

        SPONGE_TICK_RULE.evaluate(TickActivityContext(Block.SPONGE, instance, position.asBlockVec()))

        Assertions.assertTrue(instance.getBlock(position).compare(Block.WET_SPONGE))
        Assertions.assertTrue(instance.getBlock(water).compare(Block.AIR))
    }

    @Test
    fun doesNotAbsorbInfiniteWater() {
        instance.setBlock(position, Block.SPONGE)
        for (x in -5..5) {
            for (z in -5..5) {
                instance.setBlock(position.add(x.toDouble(), 0.0, z.toDouble()), Block.WATER)
            }
        }

        absorbWater(instance, position.asBlockVec())

        val remainingWater = (-5..5).sumOf { x ->
            (-5..5).count { z ->
                instance.getBlock(position.add(x.toDouble(), 0.0, z.toDouble())).compare(Block.WATER)
            }
        }
        Assertions.assertTrue(remainingWater > 0, "expected water to remain after the absorption limit")
    }
}
