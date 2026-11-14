@file:Suppress("UnstableApiUsage")

package org.everbuild.blocksandstuff.blocks.placement

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.instance.block.BlockHandler.PlayerPlacement
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.generator.Generator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class HangingSignPlacementRuleTest {
    private lateinit var instance: Instance

    private val handler =
        object : BlockHandler {
            override fun getKey(): Key = Key.key("test", "hanging_sign")
        }

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(Generator { unit: GenerationUnit -> unit.modifier().fillHeight(0, 60, Block.STONE) })
        instance.loadChunk(0, 0).join()
        MinecraftServer.getBlockManager().registerBlockPlacementRule(HangingSignPlacementRule(Block.OAK_HANGING_SIGN))
        MinecraftServer.getBlockManager().registerBlockPlacementRule(HangingSignPlacementRule(Block.OAK_WALL_HANGING_SIGN))
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun placeCeilingUnderFullBlock() {
        val player = mock<Player> { on { position } doReturn Pos(0.0, 65.0, 0.0) }
        val item = Block.OAK_HANGING_SIGN.withHandler(handler)
        instance.setBlock(Pos(0.0, 64.0, 0.0), Block.STONE)

        instance.placeBlock(
            PlayerPlacement(
                item,
                Block.AIR,
                instance,
                Pos(0.0, 63.0, 0.0),
                player,
                null,
                BlockFace.BOTTOM,
                0f,
                0f,
                0f,
            ),
        )

        val placed = instance.getBlock(Pos(0.0, 63.0, 0.0))
        Assertions.assertTrue(placed.compare(Block.OAK_HANGING_SIGN), "expected ceiling hanging sign, got ${placed.name()}")
        Assertions.assertNotNull(placed.getProperty("rotation"))
        Assertions.assertEquals("false", placed.getProperty("attached"))
    }

    @Test
    fun placeWallHangingSign() {
        val player = mock<Player> { on { position } doReturn Pos(5.0, 65.0, 5.0) }
        val item = Block.OAK_HANGING_SIGN.withHandler(handler)
        val pos = Pos(5.0, 64.0, 5.0)
        instance.setBlock(Pos(4.0, 64.0, 5.0), Block.STONE)
        instance.setBlock(Pos(6.0, 64.0, 5.0), Block.STONE)
        instance.setBlock(Pos(5.0, 64.0, 4.0), Block.STONE)
        instance.setBlock(Pos(5.0, 64.0, 6.0), Block.STONE)

        instance.placeBlock(
            PlayerPlacement(
                item,
                Block.AIR,
                instance,
                pos,
                player,
                null,
                BlockFace.NORTH,
                0f,
                0f,
                0f,
            ),
        )

        val placed = instance.getBlock(pos)
        Assertions.assertTrue(
            placed.compare(Block.OAK_WALL_HANGING_SIGN),
            "expected wall hanging sign, got ${placed.name()}",
        )
        Assertions.assertNotNull(placed.getProperty("facing"))
    }

    @Test
    fun ceilingHangingSignStaysAfterUnrelatedNeighborUpdate() {
        val player = mock<Player> { on { position } doReturn Pos(0.0, 65.0, 0.0) }
        instance.setBlock(Pos(0.0, 64.0, 0.0), Block.STONE)
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_HANGING_SIGN.withHandler(handler),
                Block.AIR,
                instance,
                Pos(0.0, 63.0, 0.0),
                player,
                null,
                BlockFace.BOTTOM,
                0f,
                0f,
                0f,
            ),
        )

        instance.setBlock(Pos(1.0, 63.0, 0.0), Block.STONE)

        Assertions.assertTrue(
            instance.getBlock(Pos(0.0, 63.0, 0.0)).compare(Block.OAK_HANGING_SIGN),
            "sign should survive an unrelated neighbor update",
        )
    }

    @Test
    fun ceilingHangingSignDropsWhenSupportRemoved() {
        val player = mock<Player> { on { position } doReturn Pos(0.0, 65.0, 0.0) }
        instance.setBlock(Pos(0.0, 64.0, 0.0), Block.STONE)
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_HANGING_SIGN.withHandler(handler),
                Block.AIR,
                instance,
                Pos(0.0, 63.0, 0.0),
                player,
                null,
                BlockFace.BOTTOM,
                0f,
                0f,
                0f,
            ),
        )
        Assertions.assertTrue(instance.getBlock(Pos(0.0, 63.0, 0.0)).compare(Block.OAK_HANGING_SIGN))

        instance.setBlock(Pos(0.0, 64.0, 0.0), Block.AIR)

        Assertions.assertTrue(
            instance.getBlock(Pos(0.0, 63.0, 0.0)).air(),
            "sign should drop after its support is removed",
        )
    }
}
