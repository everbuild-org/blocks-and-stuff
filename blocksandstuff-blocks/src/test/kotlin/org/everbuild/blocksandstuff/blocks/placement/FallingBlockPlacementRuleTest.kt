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

class FallingBlockPlacementRuleTest {
    private lateinit var instance: Instance
    private val rule = FallingBlockPlacementRule(Block.SAND)

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
    fun sandOverAirFalls() {
        val pos = Pos(8.0, 64.0, 8.0)

        val result = rule.blockPlace(placementState(pos))

        Assertions.assertTrue(result!!.compare(Block.AIR), "expected air, got ${result.name()}")
    }

    @Test
    fun supportedSandStays() {
        val pos = Pos(8.0, 64.0, 8.0)
        instance.setBlock(pos.sub(0.0, 1.0, 0.0), Block.STONE)

        val result = rule.blockPlace(placementState(pos))

        Assertions.assertTrue(result!!.compare(Block.SAND), "expected sand, got ${result.name()}")
    }

    @Test
    fun removingSupportStartsFall() {
        val pos = Pos(8.0, 64.0, 8.0)

        val result =
            rule.blockUpdate(
                BlockPlacementRule.UpdateState(instance, pos, Block.SAND, BlockFace.TOP),
            )

        Assertions.assertTrue(result.compare(Block.AIR), "expected air, got ${result.name()}")
    }

    private fun placementState(pos: Pos) =
        BlockPlacementRule.PlacementState(
            instance,
            Block.SAND,
            BlockFace.TOP,
            pos,
            null,
            null,
            null,
            false,
        )
}
