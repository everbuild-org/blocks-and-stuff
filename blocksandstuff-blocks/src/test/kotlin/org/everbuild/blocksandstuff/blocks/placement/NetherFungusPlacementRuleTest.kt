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

class NetherFungusPlacementRuleTest {
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
    fun placesOnAnyVegetationBlock() {
        instance.setBlock(position.sub(0.0, 1.0, 0.0), Block.GRASS_BLOCK)
        val rule = NetherFungusPlacementRule(Block.CRIMSON_FUNGUS)

        Assertions.assertNotNull(rule.blockPlace(placementState(Block.CRIMSON_FUNGUS)))
    }

    @Test
    fun placesOnMatchingNylium() {
        instance.setBlock(position.sub(0.0, 1.0, 0.0), Block.CRIMSON_NYLIUM)
        val rule = NetherFungusPlacementRule(Block.CRIMSON_FUNGUS)

        Assertions.assertNotNull(rule.blockPlace(placementState(Block.CRIMSON_FUNGUS)))
    }

    @Test
    fun breaksWithoutNylium() {
        val rule = NetherFungusPlacementRule(Block.CRIMSON_FUNGUS)

        val result = rule.blockUpdate(BlockPlacementRule.UpdateState(instance, position, Block.CRIMSON_FUNGUS, BlockFace.TOP))

        Assertions.assertTrue(result.air())
    }

    private fun placementState(block: Block) =
        BlockPlacementRule.PlacementState(instance, block, BlockFace.TOP, position, null, null, null, false)
}
