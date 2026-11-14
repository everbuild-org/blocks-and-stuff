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

class RedstoneStuffPlacementRuleTest {
    private lateinit var instance: Instance
    private val pos = Pos(8.0, 65.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        instance.setBlock(pos.sub(0.0, 1.0, 0.0), Block.STONE)
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun facingPointsTowardPlayer() {
        // Player looking north (yaw 180) => diode input faces the player (south), output north.
        Assertions.assertEquals("south", place(180f).getProperty("facing"))
        // Player looking south (yaw 0) => input north, output south.
        Assertions.assertEquals("north", place(0f).getProperty("facing"))
    }

    private fun place(yaw: Float): Block {
        val rule = RedstoneStuffPlacementRule(Block.REPEATER)
        return requireNotNull(
            rule.blockPlace(
                BlockPlacementRule.PlacementState(
                    instance,
                    Block.REPEATER,
                    BlockFace.TOP,
                    pos,
                    null,
                    Pos(0.0, 0.0, 0.0, yaw, 0f),
                    null,
                    false,
                ),
            ),
        )
    }
}
