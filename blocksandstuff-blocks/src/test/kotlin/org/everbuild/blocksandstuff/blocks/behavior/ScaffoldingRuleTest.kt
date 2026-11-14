package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_behaviour_block.type.PlacementActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScaffoldingRuleTest {
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
    fun becomesWaterloggedWhenPlacedIntoWater() {
        instance.setBlock(position, Block.SCAFFOLDING.withProperty("distance", "2"))

        SCAFFOLDING_PLACE_RULE.evaluate(
            PlacementActivityContext(Block.SCAFFOLDING, Block.WATER, instance, position.asBlockVec()),
        )

        val placed = instance.getBlock(position)
        Assertions.assertEquals("true", placed.getProperty("waterlogged"))
        Assertions.assertEquals("2", placed.getProperty("distance"))
    }

    @Test
    fun staysDryWhenPlacedIntoAir() {
        instance.setBlock(position, Block.SCAFFOLDING.withProperty("distance", "2"))

        SCAFFOLDING_PLACE_RULE.evaluate(
            PlacementActivityContext(Block.SCAFFOLDING, Block.AIR, instance, position.asBlockVec()),
        )

        Assertions.assertNotEquals("true", instance.getBlock(position).getProperty("waterlogged"))
    }
}
