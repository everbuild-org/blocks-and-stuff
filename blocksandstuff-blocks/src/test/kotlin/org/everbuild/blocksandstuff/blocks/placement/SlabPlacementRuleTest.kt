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
import org.mockito.kotlin.*

internal class SlabPlacementRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(Generator { unit: GenerationUnit -> unit.modifier().fillHeight(0, 60, Block.STONE) })
        instance.loadChunk(0, 0).join()
        MinecraftServer.getBlockManager().registerBlockPlacementRule(SlabPlacementRule(Block.OAK_SLAB))
    }

    @Test
    fun placeOnFaces() {
        val player = mock<Player> {
            on { position } doReturn Pos(0.0, 65.0, 0.0)
        }

        // place on top of block
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_SLAB,
                Block.AIR,
                instance,
                Pos.ZERO,
                player,
                null,
                BlockFace.TOP,
                0f,
                0f,
                0f
            )
        )

        Assertions.assertTrue(instance.getBlock(Pos.ZERO).compare(Block.OAK_SLAB))
        Assertions.assertEquals("bottom", instance.getBlock(Pos.ZERO).getProperty("type"))

        instance.setBlock(Pos.ZERO, Block.AIR)

        // place below block
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_SLAB,
                Block.AIR,
                instance,
                Pos.ZERO,
                player,
                null,
                BlockFace.BOTTOM,
                0f,
                1f,
                0f
            )
        )

        Assertions.assertTrue(instance.getBlock(Pos.ZERO).compare(Block.OAK_SLAB))
        Assertions.assertEquals("top", instance.getBlock(Pos.ZERO).getProperty("type"))

        instance.setBlock(Pos.ZERO, Block.AIR)

        // side bottom
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_SLAB,
                Block.AIR,
                instance,
                Pos.ZERO,
                player,
                null,
                BlockFace.EAST,
                0f,
                0f,
                0f
            )
        )

        Assertions.assertTrue(instance.getBlock(Pos.ZERO).compare(Block.OAK_SLAB))
        Assertions.assertEquals("bottom", instance.getBlock(Pos.ZERO).getProperty("type"))

        instance.setBlock(Pos.ZERO, Block.AIR)

        // side top
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_SLAB,
                Block.AIR,
                instance,
                Pos.ZERO,
                player,
                null,
                BlockFace.EAST,
                0f,
                1f,
                0f
            )
        )

        Assertions.assertTrue(instance.getBlock(Pos.ZERO).compare(Block.OAK_SLAB))
        Assertions.assertEquals("top", instance.getBlock(Pos.ZERO).getProperty("type"))
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }
}