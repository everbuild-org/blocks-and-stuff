package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class PointedDripstoneBehaviourTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)
    private val rule: Rule<RandomTickActivityContext> =
        PointedDripstoneBehaviour(
            object : Random() {
                override fun nextDouble(): Double = 0.0

                override fun nextFloat(): Float = 0.0f

                override fun nextBoolean(): Boolean = true

                override fun nextInt(bound: Int): Int = 0
            },
        ).rule("test:pointed_dripstone")

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

    private fun placeColumn(
        source: Block,
        support: Block = Block.DRIPSTONE_BLOCK,
        thickness: String = "tip",
    ) {
        instance.setBlock(position.add(0.0, 1.0, 0.0), support)
        instance.setBlock(position.add(0.0, 2.0, 0.0), source)
        instance.setBlock(position, tip(thickness))
    }

    private fun tip(thickness: String = "tip"): Block =
        Block.POINTED_DRIPSTONE
            .withProperty("vertical_direction", "down")
            .withProperty("thickness", thickness)
            .withProperty("waterlogged", "false")

    private fun tick() {
        rule.evaluate(RandomTickActivityContext(instance.getBlock(position), instance, position.asBlockVec()))
    }

    @Test
    fun stalactiteGrowsDown() {
        placeColumn(Block.WATER)
        tick()

        val grown = instance.getBlock(position.add(0.0, -1.0, 0.0))
        Assertions.assertEquals(Block.POINTED_DRIPSTONE.key(), grown.key())
        Assertions.assertEquals("down", grown.getProperty("vertical_direction"))
        Assertions.assertEquals("tip", grown.getProperty("thickness"))
    }

    @Test
    fun waterDripFillsCauldron() {
        placeColumn(Block.WATER)
        val cauldronPosition = position.add(0.0, -4.0, 0.0)
        instance.setBlock(cauldronPosition, Block.CAULDRON)
        tick()

        val cauldron = instance.getBlock(cauldronPosition)
        Assertions.assertEquals(Block.WATER_CAULDRON.key(), cauldron.key())
        Assertions.assertEquals("1", cauldron.getProperty("level"))
    }

    @Test
    fun lavaDripFillsCauldron() {
        placeColumn(Block.LAVA)
        val cauldronPosition = position.add(0.0, -4.0, 0.0)
        instance.setBlock(cauldronPosition, Block.CAULDRON)
        tick()

        Assertions.assertEquals(Block.LAVA_CAULDRON.key(), instance.getBlock(cauldronPosition).key())
    }

    @Test
    fun flowingLavaAboveTipFillsCauldron() {
        placeColumn(Block.LAVA.withProperty("level", "1"))
        val cauldronPosition = position.add(0.0, -4.0, 0.0)
        instance.setBlock(cauldronPosition, Block.CAULDRON)
        tick()

        Assertions.assertEquals(Block.LAVA_CAULDRON.key(), instance.getBlock(cauldronPosition).key())
    }

    @Test
    fun waterDripTurnsMudIntoClay() {
        placeColumn(Block.MUD)
        tick()

        Assertions.assertEquals(Block.CLAY.key(), instance.getBlock(position.add(0.0, 2.0, 0.0)).key())
    }

    @Test
    fun waterloggedTipDoesNotDrip() {
        placeColumn(Block.WATER)
        instance.setBlock(position, tip().withProperty("waterlogged", "true"))
        val cauldronPosition = position.add(0.0, -4.0, 0.0)
        instance.setBlock(cauldronPosition, Block.CAULDRON)
        tick()

        Assertions.assertEquals(Block.CAULDRON.key(), instance.getBlock(cauldronPosition).key())
    }
}
