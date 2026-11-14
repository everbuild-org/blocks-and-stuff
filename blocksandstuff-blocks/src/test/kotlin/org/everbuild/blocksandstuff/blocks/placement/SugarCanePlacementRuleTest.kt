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

class SugarCanePlacementRuleTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)
    private val rule = SugarCanePlacementRule(Block.SUGAR_CANE)

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
    fun placesNextToWaterBesideGround() {
        instance.setBlock(position.sub(0.0, 1.0, 0.0), Block.SAND)
        instance.setBlock(position.sub(0.0, 1.0, 0.0).add(1.0, 0.0, 0.0), Block.WATER)

        Assertions.assertNotNull(rule.blockPlace(placementState()))
    }

    @Test
    fun placesNextToWaterBesideCane() {
        instance.setBlock(position.sub(0.0, 1.0, 0.0), Block.SAND)
        instance.setBlock(position.add(1.0, 0.0, 0.0), Block.WATER)

        Assertions.assertNotNull(rule.blockPlace(placementState()))
    }

    @Test
    fun doesNotPlaceWithoutWater() {
        instance.setBlock(position.sub(0.0, 1.0, 0.0), Block.SAND)

        Assertions.assertNull(rule.blockPlace(placementState()))
    }

    private fun placementState() =
        BlockPlacementRule.PlacementState(
            instance,
            Block.SUGAR_CANE,
            BlockFace.TOP,
            position,
            null,
            null,
            null,
            false,
        )
}
