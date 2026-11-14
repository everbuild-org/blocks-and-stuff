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

internal class StairsPlacementRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(Generator { unit: GenerationUnit -> unit.modifier().fillHeight(0, 60, Block.STONE) })
        instance.loadChunk(0, 0).join()
        MinecraftServer.getBlockManager().registerBlockPlacementRule(StairsPlacementRule(Block.OAK_STAIRS))
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    private fun place(pos: Pos, face: BlockFace, cursorY: Float) {
        val player = mock<Player> { on { position } doReturn Pos(0.0, 65.0, 0.0, 0f, 0f) }
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_STAIRS,
                Block.AIR,
                instance,
                pos,
                player,
                null,
                face,
                0f,
                cursorY,
                0f,
            ),
        )
    }

    @Test
    fun halfDependsOnClickedFaceAndCursor() {
        val top = Pos(4.0, 64.0, 4.0)
        place(top, BlockFace.TOP, 0f)
        Assertions.assertEquals("bottom", instance.getBlock(top).getProperty("half"))

        val bottom = Pos(6.0, 64.0, 4.0)
        place(bottom, BlockFace.BOTTOM, 0f)
        Assertions.assertEquals("top", instance.getBlock(bottom).getProperty("half"))

        val highSide = Pos(8.0, 64.0, 4.0)
        place(highSide, BlockFace.EAST, 0.8f)
        Assertions.assertEquals("top", instance.getBlock(highSide).getProperty("half"))
    }

    @Test
    fun straightShapeWithoutNeighbours() {
        val pos = Pos(4.0, 64.0, 4.0)
        place(pos, BlockFace.TOP, 0f)

        val placed = instance.getBlock(pos)
        val facing = placed.getProperty("facing")
        Assertions.assertTrue(
            facing == "north" || facing == "south" || facing == "east" || facing == "west",
            "unexpected facing $facing",
        )
        Assertions.assertEquals("straight", placed.getProperty("shape"))
    }
}
