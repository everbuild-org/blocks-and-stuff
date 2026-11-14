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

class PointedDripstonePlacementRuleTest {
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
    fun sulfurSpikePlacesUp() {
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
        val rule = PointedDripstonePlacementRule(Block.SULFUR_SPIKE)

        val result = rule.blockPlace(placementState(Block.SULFUR_SPIKE, BlockFace.TOP))

        Assertions.assertNotNull(result)
        Assertions.assertEquals("up", result!!.getProperty("vertical_direction"))
    }

    @Test
    fun sulfurSpikePlacesDown() {
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.STONE)
        val rule = PointedDripstonePlacementRule(Block.SULFUR_SPIKE)

        val result = rule.blockPlace(placementState(Block.SULFUR_SPIKE, BlockFace.BOTTOM))

        Assertions.assertNotNull(result)
        Assertions.assertEquals("down", result!!.getProperty("vertical_direction"))
    }

    @Test
    fun sulfurSpikeDoesNotPlaceWithoutSupport() {
        val rule = PointedDripstonePlacementRule(Block.SULFUR_SPIKE)

        val result = rule.blockPlace(placementState(Block.SULFUR_SPIKE, BlockFace.TOP))

        Assertions.assertNull(result)
    }

    private fun placementState(
        block: Block,
        face: BlockFace,
    ) = BlockPlacementRule.PlacementState(
        instance,
        block,
        face,
        position.asBlockVec(),
        null,
        null,
        null,
        false,
    )
}
