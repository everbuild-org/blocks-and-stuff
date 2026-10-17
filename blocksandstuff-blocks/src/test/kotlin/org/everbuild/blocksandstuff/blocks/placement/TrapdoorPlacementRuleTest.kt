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

internal class TrapdoorPlacementRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(Generator { unit: GenerationUnit -> unit.modifier().fillHeight(0, 60, Block.STONE) })
        instance.loadChunk(0, 0).join()
        MinecraftServer.getBlockManager().registerBlockPlacementRule(TrapdoorPlacementRule(Block.OAK_TRAPDOOR))
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    private fun player(yaw: Float) = mock<Player> { on { position } doReturn Pos(0.0, 65.0, 0.0, yaw, 0f) }

    private fun place(pos: Pos, face: BlockFace, cursorY: Float) {
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_TRAPDOOR,
                Block.AIR,
                instance,
                pos,
                player(0f),
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
        val top = Pos(0.0, 64.0, 0.0)
        place(top, BlockFace.TOP, 0f)
        Assertions.assertEquals("bottom", instance.getBlock(top).getProperty("half"))

        val bottom = Pos(2.0, 64.0, 0.0)
        place(bottom, BlockFace.BOTTOM, 0f)
        Assertions.assertEquals("top", instance.getBlock(bottom).getProperty("half"))

        val lowSide = Pos(4.0, 64.0, 0.0)
        place(lowSide, BlockFace.EAST, 0.3f)
        Assertions.assertEquals("bottom", instance.getBlock(lowSide).getProperty("half"))

        val highSide = Pos(6.0, 64.0, 0.0)
        place(highSide, BlockFace.EAST, 0.7f)
        Assertions.assertEquals("top", instance.getBlock(highSide).getProperty("half"))
    }

    @Test
    fun facingFollowsPlayerYaw() {
        val pos = Pos(0.0, 64.0, 0.0)
        place(pos, BlockFace.TOP, 0f)
        Assertions.assertEquals("north", instance.getBlock(pos).getProperty("facing"))

        val other = Pos(2.0, 64.0, 0.0)
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_TRAPDOOR,
                Block.AIR,
                instance,
                other,
                player(90f),
                null,
                BlockFace.TOP,
                0f,
                0f,
                0f,
            ),
        )
        Assertions.assertEquals("east", instance.getBlock(other).getProperty("facing"))
    }
}
