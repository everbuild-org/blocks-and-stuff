package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LeavesDecayBehaviourTest {
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

    private fun rule() = LeavesDecayBehaviour().rule("test:leaves")

    @Test
    fun isolatedLeavesDecay() {
        val leaves = Block.OAK_LEAVES
        instance.setBlock(position, leaves)

        rule().evaluate(context(leaves))

        Assertions.assertTrue(instance.getBlock(position).air())
    }

    @Test
    fun leavesNearLogSurvive() {
        val leaves = Block.OAK_LEAVES
        instance.setBlock(position, leaves)
        instance.setBlock(position.add(1.0, 0.0, 0.0), Block.OAK_LOG)

        rule().evaluate(context(leaves))

        Assertions.assertEquals(Block.OAK_LEAVES.key(), instance.getBlock(position).key())
    }

    @Test
    fun persistentLeavesNeverDecay() {
        val leaves = Block.OAK_LEAVES.withProperty("persistent", "true")
        instance.setBlock(position, leaves)

        rule().evaluate(context(leaves))

        Assertions.assertEquals(Block.OAK_LEAVES.key(), instance.getBlock(position).key())
    }

    @Test
    fun logWithinSixLeafStepsPreventsDecay() {
        val leaves = Block.OAK_LEAVES
        for (distance in 0..5) {
            instance.setBlock(position.add(distance.toDouble(), 0.0, 0.0), leaves)
        }
        instance.setBlock(position.add(6.0, 0.0, 0.0), Block.OAK_LOG)

        rule().evaluate(context(leaves))

        Assertions.assertEquals(Block.OAK_LEAVES.key(), instance.getBlock(position).key())
    }

    @Test
    fun logSevenLeafStepsAwayDoesNotPreventDecay() {
        val leaves = Block.OAK_LEAVES
        for (distance in 0..6) {
            instance.setBlock(position.add(distance.toDouble(), 0.0, 0.0), leaves)
        }
        instance.setBlock(position.add(7.0, 0.0, 0.0), Block.OAK_LOG)

        rule().evaluate(context(leaves))

        Assertions.assertTrue(instance.getBlock(position).air())
    }

    @Test
    fun leavesTagIsResolved() {
        Assertions.assertTrue(BlockTags.getTaggedWith("minecraft:leaves").isNotEmpty())
    }
}
