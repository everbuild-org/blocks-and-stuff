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

class HeadPlacementRuleTest {
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
    fun dragonHeadPlacesWallVariantOnSide() {
        val rule = HeadPlacementRule(Block.DRAGON_HEAD)

        val result =
            rule.blockPlace(
                BlockPlacementRule.PlacementState(instance, Block.DRAGON_HEAD, BlockFace.NORTH, position, null, null, null, false),
            )

        Assertions.assertEquals(Block.DRAGON_WALL_HEAD.key(), result?.key())
    }

    @Test
    fun dragonHeadPlacesOnTop() {
        val rule = HeadPlacementRule(Block.DRAGON_HEAD)

        val result =
            rule.blockPlace(
                BlockPlacementRule.PlacementState(instance, Block.DRAGON_HEAD, BlockFace.TOP, position, null, null, null, false),
            )

        Assertions.assertEquals(Block.DRAGON_HEAD.key(), result?.key())
    }
}
