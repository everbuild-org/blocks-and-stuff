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
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.utils.rotateL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class FenceGatePlacementRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.setGenerator(Generator { unit: GenerationUnit -> unit.modifier().fillHeight(0, 60, Block.STONE) })
        instance.loadChunk(0, 0).join()
        MinecraftServer.getBlockManager().registerBlockPlacementRule(FenceGatePlacementRule(Block.OAK_FENCE_GATE))
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    private fun place(pos: Pos, yaw: Float) {
        val player = mock<Player> { on { position } doReturn Pos(0.0, 65.0, 0.0, yaw, 0f) }
        instance.placeBlock(
            PlayerPlacement(
                Block.OAK_FENCE_GATE,
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
    fun facingIsHorizontal() {
        val pos = Pos(4.0, 64.0, 4.0)
        place(pos, 0f)

        val facing = instance.getBlock(pos).getProperty("facing")
        Assertions.assertTrue(
            facing == "north" || facing == "south",
            "gate facing should be on the north/south axis for yaw 0, was $facing",
        )
    }

    @Test
    fun inWallTurnsTrueWhenNeighbouringWall() {
        val pos = Pos(8.0, 64.0, 8.0)
        place(pos, 0f)
        Assertions.assertEquals("false", instance.getBlock(pos).getProperty("in_wall"))

        val facing = Direction.valueOf(instance.getBlock(pos).getProperty("facing")!!.uppercase())
        val perpendicular = pos.add(facing.rotateL().vec())
        instance.setBlock(perpendicular, Block.COBBLESTONE_WALL)

        Assertions.assertEquals("true", instance.getBlock(pos).getProperty("in_wall"))
    }
}
