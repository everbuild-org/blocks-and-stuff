package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class PlantGrowthBehaviourTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            MinecraftServer.init()
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            MinecraftServer.stopCleanly()
        }
    }

    private fun context(block: Block): RandomTickActivityContext = RandomTickActivityContext(block, mock<Instance>(), BlockVec(0, 64, 0))

    @Test
    fun advancesAgeByOne() {
        val rule = PlantGrowthBehaviour(growthChance = 1, minLight = 0).rule("test:growth")
        val ctx = context(Block.WHEAT.withProperty("age", "0"))

        rule.evaluate(ctx)

        Assertions.assertEquals("1", ctx.newBlock?.getProperty("age"))
    }

    @Test
    fun doesNotGrowPastMaxAge() {
        val rule = PlantGrowthBehaviour(growthChance = 1, minLight = 0).rule("test:growth")
        val ctx = context(Block.WHEAT.withProperty("age", "7"))

        rule.evaluate(ctx)

        Assertions.assertNull(ctx.newBlock)
    }

    @Test
    fun doesNotGrowWithoutLight() {
        val rule = PlantGrowthBehaviour(growthChance = 1, minLight = 9).rule("test:growth")
        val instance = mock<Instance>()
        val ctx = RandomTickActivityContext(Block.WHEAT.withProperty("age", "0"), instance, BlockVec(0, 64, 0))

        rule.evaluate(ctx)

        Assertions.assertNull(ctx.newBlock)
    }

    @Test
    fun registersGrowthForCrops() {
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addPlantGrowth() })

        Assertions.assertNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:wheat"))
        Assertions.assertNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:nether_wart"))
    }
}
