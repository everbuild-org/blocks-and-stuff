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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class BellPlacementRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(Generator { unit: GenerationUnit -> unit.modifier().fillHeight(0, 60, Block.STONE) })
        instance.loadChunk(0, 0).join()
        MinecraftServer.getBlockManager().registerBlockPlacementRule(BellPlacementRule(Block.BELL))
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    private fun place(pos: Pos) {
        val player = mock<Player> { on { position } doReturn Pos(pos.x(), pos.y(), pos.z(), 0f, 0f) }
        instance.placeBlock(
            PlayerPlacement(
                Block.BELL,
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
    fun floorBellSurvivesInitialUpdate() {
        val pos = Pos(4.0, 60.0, 4.0)
        place(pos)

        Assertions.assertEquals(Block.BELL.key(), instance.getBlock(pos).key())
        Assertions.assertEquals("floor", instance.getBlock(pos).getProperty("attachment"))
    }

    @Test
    fun floorBellSurvivesNeighborPlacement() {
        val pos = Pos(4.0, 60.0, 4.0)
        place(pos)

        instance.setBlock(pos.add(1.0, 0.0, 0.0), Block.STONE)
        instance.setBlock(pos.add(0.0, 0.0, 1.0), Block.STONE)

        Assertions.assertEquals(Block.BELL.key(), instance.getBlock(pos).key())
    }

    @Test
    fun floorBellIsRemovedWhenSupportDisappears() {
        val pos = Pos(4.0, 60.0, 4.0)
        place(pos)
        Assertions.assertEquals(Block.BELL.key(), instance.getBlock(pos).key())

        instance.setBlock(pos.add(0.0, -1.0, 0.0), Block.AIR)

        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(pos).key())
    }
}
