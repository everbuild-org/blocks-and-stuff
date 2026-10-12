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

internal class FacedFacingPlacementRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(Generator { unit: GenerationUnit -> unit.modifier().fillHeight(0, 60, Block.STONE) })
        instance.loadChunk(0, 0).join()
        MinecraftServer.getBlockManager().registerBlockPlacementRule(FacedFacingPlacementRule(Block.OAK_BUTTON))
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    private fun place(pos: Pos, face: BlockFace) {
        val player = mock<Player> { on { position } doReturn Pos(0.0, 65.0, 0.0, 0f, 0f) }
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_BUTTON,
                Block.AIR,
                instance,
                pos,
                player,
                null,
                face,
                0f,
                0f,
                0f,
            ),
        )
    }

    @Test
    fun floorPlacement() {
        val pos = Pos(4.0, 64.0, 4.0)
        instance.setBlock(Pos(4.0, 63.0, 4.0), Block.STONE)
        place(pos, BlockFace.TOP)

        val placed = instance.getBlock(pos)
        Assertions.assertTrue(placed.compare(Block.OAK_BUTTON))
        Assertions.assertEquals("floor", placed.getProperty("face"))
        Assertions.assertNotNull(placed.getProperty("facing"))
    }

    @Test
    fun ceilingPlacement() {
        val pos = Pos(6.0, 64.0, 4.0)
        instance.setBlock(Pos(6.0, 65.0, 4.0), Block.STONE)
        place(pos, BlockFace.BOTTOM)

        val placed = instance.getBlock(pos)
        Assertions.assertTrue(placed.compare(Block.OAK_BUTTON))
        Assertions.assertEquals("ceiling", placed.getProperty("face"))
    }

    @Test
    fun wallPlacementUsesClickedFace() {
        val pos = Pos(8.0, 64.0, 4.0)
        instance.setBlock(Pos(7.0, 64.0, 4.0), Block.STONE)
        place(pos, BlockFace.EAST)

        val placed = instance.getBlock(pos)
        Assertions.assertTrue(placed.compare(Block.OAK_BUTTON))
        Assertions.assertEquals("wall", placed.getProperty("face"))
        Assertions.assertEquals("east", placed.getProperty("facing"))
    }

    @Test
    fun placementWithoutSupportIsRejected() {
        val pos = Pos(10.0, 64.0, 4.0)
        place(pos, BlockFace.TOP)

        Assertions.assertTrue(instance.getBlock(pos).air(), "button without support should not be placed")
    }
}
