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

internal class GroundedPlantBlockPlacementRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(Generator { unit: GenerationUnit -> unit.modifier().fillHeight(0, 60, Block.STONE) })
        instance.loadChunk(0, 0).join()
        MinecraftServer.getBlockManager().registerBlockPlacementRule(GroundedPlantBlockPlacementRule(Block.DANDELION))
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    private fun place(pos: Pos) {
        val player = mock<Player> { on { position } doReturn Pos(0.0, 65.0, 0.0, 0f, 0f) }
        instance.placeBlock(
            PlayerPlacement(
                Block.DANDELION,
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
    fun placesOnVegetationSupport() {
        val pos = Pos(0.0, 64.0, 0.0)
        instance.setBlock(Pos(0.0, 63.0, 0.0), Block.DIRT)
        place(pos)

        Assertions.assertTrue(instance.getBlock(pos).compare(Block.DANDELION))
    }

    @Test
    fun rejectsOnInvalidSupport() {
        val pos = Pos(2.0, 64.0, 0.0)
        instance.setBlock(Pos(2.0, 63.0, 0.0), Block.STONE)
        place(pos)

        Assertions.assertTrue(instance.getBlock(pos).air(), "plant should not be placed on stone")
    }

    @Test
    fun dropsWhenSupportRemoved() {
        val pos = Pos(0.0, 64.0, 0.0)
        instance.setBlock(Pos(0.0, 63.0, 0.0), Block.DIRT)
        place(pos)
        Assertions.assertTrue(instance.getBlock(pos).compare(Block.DANDELION))

        instance.setBlock(Pos(0.0, 63.0, 0.0), Block.STONE)

        Assertions.assertTrue(instance.getBlock(pos).air(), "plant should drop when support is removed")
    }
}
