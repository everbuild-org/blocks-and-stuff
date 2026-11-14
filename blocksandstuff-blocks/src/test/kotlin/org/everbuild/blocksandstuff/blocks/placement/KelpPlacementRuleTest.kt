package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KelpPlacementRuleTest {
    private lateinit var instance: Instance
    private val position = Pos(4.0, 64.0, 4.0)

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    private fun update(
        rule: KelpPlacementRule,
        block: Block,
    ): Block =
        rule.blockUpdate(
            BlockPlacementRule.UpdateState(instance, position.asBlockVec(), block, BlockFace.TOP),
        )

    @Test
    fun bodyBecomesHeadWhenTopIsBroken() {
        val rule = KelpPlacementRule(Block.KELP_PLANT)
        instance.setBlock(position.relative(BlockFace.BOTTOM), Block.SAND)
        instance.setBlock(position, Block.KELP_PLANT)

        val result = update(rule, Block.KELP_PLANT)

        Assertions.assertEquals(Block.KELP.key(), result.key())
        Assertions.assertNotNull(result.getProperty("age"))
    }

    @Test
    fun bodyStaysBodyWhileKelpIsAbove() {
        val rule = KelpPlacementRule(Block.KELP_PLANT)
        instance.setBlock(position.relative(BlockFace.BOTTOM), Block.SAND)
        instance.setBlock(position.relative(BlockFace.TOP), Block.KELP.withProperty("age", "3"))

        val result = update(rule, Block.KELP_PLANT)

        Assertions.assertEquals(Block.KELP_PLANT.key(), result.key())
    }

    @Test
    fun kelpBreaksWithoutSupport() {
        val rule = KelpPlacementRule(Block.KELP)

        val result = update(rule, Block.KELP)

        Assertions.assertEquals(Block.WATER.key(), result.key())
    }
}
