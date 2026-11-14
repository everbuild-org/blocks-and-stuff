package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class KelpAndSeagrassBehaviourTest {
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

    private fun context(block: Block): RandomTickActivityContext =
        RandomTickActivityContext(block, instance, position.asBlockVec())

    private fun evaluate(
        rule: org.everbuild.twaddle.stacked_behaviour.Rule<RandomTickActivityContext>,
        block: Block,
    ) {
        val ctx = context(block)
        rule.evaluate(ctx)
        ctx.newBlock?.let { instance.setBlock(position, it) }
    }

    private fun alwaysGrowRandom(): Random =
        object : Random() {
            override fun nextDouble(): Double = 0.0

            override fun nextInt(bound: Int): Int = 0
        }

    @Test
    fun kelpGrowsUpwardsAndConvertsHeadToPlant() {
        val rule = KelpGrowthBehaviour(alwaysGrowRandom()).rule("test:kelp")
        val kelp = Block.KELP.withProperty("age", "0")
        instance.setBlock(position, kelp)
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.WATER)

        evaluate(rule, kelp)

        Assertions.assertEquals(Block.KELP_PLANT.key(), instance.getBlock(position).key())
        val grown = instance.getBlock(position.add(0.0, 1.0, 0.0))
        Assertions.assertEquals(Block.KELP.key(), grown.key())
        Assertions.assertEquals("1", grown.getProperty("age"))
    }

    @Test
    fun kelpDoesNotGrowWithoutWaterAbove() {
        val rule = KelpGrowthBehaviour(alwaysGrowRandom()).rule("test:kelp")
        val kelp = Block.KELP.withProperty("age", "0")
        instance.setBlock(position, kelp)

        evaluate(rule, kelp)

        Assertions.assertEquals(Block.KELP.key(), instance.getBlock(position).key())
    }

    @Test
    fun kelpStopsAtMaxAge() {
        val rule = KelpGrowthBehaviour(alwaysGrowRandom()).rule("test:kelp")
        val kelp = Block.KELP.withProperty("age", "25")
        instance.setBlock(position, kelp)
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.WATER)

        evaluate(rule, kelp)

        Assertions.assertTrue(instance.getBlock(position.relative(BlockFace.TOP)).compare(Block.WATER))
    }

    @Test
    fun supportedSeagrassSurvives() {
        val rule = SeagrassSurvivalBehaviour().rule("test:seagrass")
        val seagrass = Block.SEAGRASS
        instance.setBlock(position, seagrass)
        instance.setBlock(position.relative(BlockFace.BOTTOM), Block.SAND)

        evaluate(rule, seagrass)

        Assertions.assertEquals(Block.SEAGRASS.key(), instance.getBlock(position).key())
    }

    @Test
    fun unsupportedSeagrassTurnsIntoWater() {
        val rule = SeagrassSurvivalBehaviour().rule("test:seagrass")
        val seagrass = Block.SEAGRASS
        instance.setBlock(position, seagrass)

        evaluate(rule, seagrass)

        Assertions.assertEquals(Block.WATER.key(), instance.getBlock(position).key())
    }
}
