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

class RedstoneWirePlacementRuleTest {
    private lateinit var instance: Instance
    private val rule = RedstoneWirePlacementRule(Block.REDSTONE_WIRE)
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
    fun connectsToRedstoneLamp() {
        instance.setBlock(pos.add(1.0, 0.0, 0.0), Block.REDSTONE_LAMP)
        instance.setBlock(pos.sub(1.0, 0.0, 0.0), Block.GLASS)

        val result = rule.blockUpdate(BlockPlacementRule.UpdateState(instance, pos, Block.REDSTONE_WIRE, BlockFace.TOP))

        Assertions.assertEquals("side", result.getProperty("east"))
        Assertions.assertEquals("none", result.getProperty("north"))
    }

    @Test
    fun plusJunctionConnectsSideways() {
        for (p in listOf(
            pos,
            pos.add(1.0, 0.0, 0.0),
            pos.sub(1.0, 0.0, 0.0),
            pos.add(0.0, 0.0, 1.0),
            pos.sub(0.0, 0.0, 1.0),
        )) {
            instance.setBlock(p, Block.REDSTONE_WIRE)
        }

        val result = rule.blockUpdate(BlockPlacementRule.UpdateState(instance, pos, instance.getBlock(pos), BlockFace.TOP))

        Assertions.assertEquals("side", result.getProperty("east"))
        Assertions.assertEquals("side", result.getProperty("west"))
        Assertions.assertEquals("side", result.getProperty("north"))
        Assertions.assertEquals("side", result.getProperty("south"))
    }

    @Test
    fun sideConnectionWinsOverStaircase() {
        instance.setBlock(pos, Block.REDSTONE_WIRE)
        instance.setBlock(pos.add(1.0, 0.0, 0.0), Block.REDSTONE_WIRE)
        instance.setBlock(pos.add(1.0, 1.0, 0.0), Block.REDSTONE_WIRE)

        val result = rule.blockUpdate(BlockPlacementRule.UpdateState(instance, pos, instance.getBlock(pos), BlockFace.TOP))

        Assertions.assertEquals("side", result.getProperty("east"))
    }

    @Test
    fun doesNotConnectToPlainBlock() {
        instance.setBlock(pos.add(1.0, 0.0, 0.0), Block.REDSTONE_LAMP)
        instance.setBlock(pos.sub(1.0, 0.0, 0.0), Block.REDSTONE_LAMP)
        instance.setBlock(pos.sub(0.0, 0.0, 1.0), Block.STONE)

        val result = rule.blockUpdate(BlockPlacementRule.UpdateState(instance, pos, Block.REDSTONE_WIRE, BlockFace.TOP))

        Assertions.assertEquals("side", result.getProperty("east"))
        Assertions.assertEquals("side", result.getProperty("west"))
        Assertions.assertEquals("none", result.getProperty("north"))
    }
}
