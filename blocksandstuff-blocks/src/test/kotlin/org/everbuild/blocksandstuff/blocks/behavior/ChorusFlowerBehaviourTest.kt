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

class ChorusFlowerBehaviourTest {
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

    private fun maxRandom(): Random =
        object : Random() {
            override fun nextInt(bound: Int): Int = bound - 1
        }

    @Test
    fun growsUpwardsOnEndStone() {
        val rule = ChorusFlowerBehaviour().rule("test:chorus")
        val flower = Block.CHORUS_FLOWER.withProperty("age", "0")
        instance.setBlock(position, flower)
        instance.setBlock(position.relative(BlockFace.BOTTOM), Block.END_STONE)

        evaluate(rule, flower)

        val plant = instance.getBlock(position)
        Assertions.assertEquals(Block.CHORUS_PLANT.key(), plant.key())
        Assertions.assertEquals("true", plant.getProperty("up"))
        Assertions.assertEquals("true", plant.getProperty("down"))
        val grown = instance.getBlock(position.relative(BlockFace.TOP))
        Assertions.assertEquals(Block.CHORUS_FLOWER.key(), grown.key())
        Assertions.assertEquals("0", grown.getProperty("age"))
    }

    @Test
    fun branchesWhenUpwardGrowthIsBlocked() {
        val rule = ChorusFlowerBehaviour(maxRandom()).rule("test:chorus")
        val flower = Block.CHORUS_FLOWER.withProperty("age", "0")
        instance.setBlock(position, flower)
        instance.setBlock(position.relative(BlockFace.BOTTOM), Block.END_STONE)
        instance.setBlock(position.relative(BlockFace.TOP).relative(BlockFace.NORTH), Block.STONE)

        evaluate(rule, flower)

        Assertions.assertEquals(Block.CHORUS_PLANT.key(), instance.getBlock(position).key())
        val branches =
            listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST).count { face ->
                val branch = instance.getBlock(position.relative(face))
                branch.compare(Block.CHORUS_FLOWER) && branch.getProperty("age") == "1"
            }
        Assertions.assertTrue(branches > 0, "expected a horizontal chorus flower branch")
    }

    @Test
    fun matureFlowerDiesWhenItCannotGrow() {
        val rule = ChorusFlowerBehaviour().rule("test:chorus")
        val flower = Block.CHORUS_FLOWER.withProperty("age", "4")
        instance.setBlock(position, flower)
        instance.setBlock(position.relative(BlockFace.BOTTOM), Block.STONE)

        evaluate(rule, flower)

        Assertions.assertEquals("5", instance.getBlock(position).getProperty("age"))
    }
}
