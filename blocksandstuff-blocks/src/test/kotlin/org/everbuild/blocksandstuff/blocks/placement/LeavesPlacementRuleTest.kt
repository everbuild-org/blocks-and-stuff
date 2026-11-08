@file:Suppress("UnstableApiUsage")

package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler.PlayerPlacement
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.generator.Generator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

internal class LeavesPlacementRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(Generator { unit: GenerationUnit -> unit.modifier().fillHeight(0, 60, Block.STONE) })
        instance.loadChunk(0, 0).join()
        MinecraftServer.getBlockManager().registerBlockPlacementRule(LeavesPlacementRule(Block.OAK_LEAVES))
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    private fun place(pos: Pos) {
        val player = mock<Player>()
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_LEAVES,
                Block.AIR,
                instance,
                pos,
                player,
                null,
                BlockFace.TOP,
                0f,
                0f,
                0f,
            ),
        )
    }

    @Test
    fun placedLeavesArePersistent() {
        val pos = Pos(4.0, 64.0, 4.0)
        place(pos)

        Assertions.assertEquals("true", instance.getBlock(pos).getProperty("persistent"))
    }
}
