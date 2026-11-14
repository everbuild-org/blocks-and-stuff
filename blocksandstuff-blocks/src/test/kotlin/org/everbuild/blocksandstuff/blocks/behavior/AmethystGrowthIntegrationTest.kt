package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.blocks.group.VanillaBlockBehaviour
import org.everbuild.twaddle.common.block.tick.RandomTickHandler
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AmethystGrowthIntegrationTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        BlockRuleset.applyRulesAsDefault(VanillaBlockBehaviour.RULESET)
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun buddingAmethystGrowsThroughRegisteredHandler() {
        val handler =
            Assertions.assertInstanceOf(
                RandomTickHandler::class.java,
                MinecraftServer.getBlockManager().getHandler("minecraft:budding_amethyst"),
            )
        instance.setBlock(position, Block.BUDDING_AMETHYST)

        repeat(500) {
            handler.onRandomTick(
                RandomTickHandler.RandomTick(
                    Block.BUDDING_AMETHYST,
                    instance,
                    position.asBlockVec(),
                ),
            )
        }

        val clusters =
            BlockFace.entries.count { face ->
                instance.getBlock(position.relative(face)).compare(Block.AMETHYST_CLUSTER)
            }
        Assertions.assertTrue(clusters > 0, "expected at least one full amethyst cluster")
    }
}
