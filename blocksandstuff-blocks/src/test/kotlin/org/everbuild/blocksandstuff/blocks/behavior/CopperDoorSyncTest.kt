package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CopperDoorSyncTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        CopperDoorSync.enable()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun changingOneHalfUpdatesTheOther() {
        val lower = Block.COPPER_DOOR.withProperty("half", "lower")
        val upper = Block.COPPER_DOOR.withProperty("half", "upper")
        instance.setBlock(position, lower)
        instance.setBlock(position.add(0.0, 1.0, 0.0), upper)

        instance.setBlock(position, Block.EXPOSED_COPPER_DOOR.withProperty("half", "lower"))

        Assertions.assertEquals(
            Block.EXPOSED_COPPER_DOOR.key(),
            instance.getBlock(position.add(0.0, 1.0, 0.0)).key(),
        )
    }

    @Test
    fun differentOxidationDoesNotBreakOtherHalf() {
        val lower = Block.COPPER_DOOR.withProperty("half", "lower")
        val upper = Block.EXPOSED_COPPER_DOOR.withProperty("half", "upper")
        instance.setBlock(position, lower)
        instance.setBlock(position.add(0.0, 1.0, 0.0), upper)

        val result =
            org.everbuild.blocksandstuff.blocks.placement.DoorPlacementRule(Block.COPPER_DOOR)
                .blockUpdate(
                    net.minestom.server.instance.block.rule.BlockPlacementRule.UpdateState(
                        instance,
                        position.add(0.0, 1.0, 0.0),
                        upper,
                        net.minestom.server.instance.block.BlockFace.TOP,
                    ),
                )

        Assertions.assertEquals(Block.EXPOSED_COPPER_DOOR.key(), result.key())
    }
}
