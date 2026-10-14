package org.everbuild.blocksandstuff.blocks.behavior

// <@AI_UNREVIEWED>
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PressurePlateRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addPressurePlates() })
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun emptyPressurePlateLosesPower() {
        val handler = handlerFor("minecraft:oak_pressure_plate")
        val pos = Pos(8.0, 64.0, 8.0)
        val plate = Block.OAK_PRESSURE_PLATE.withProperty("powered", "true").withHandler(handler)
        instance.setBlock(pos, plate)

        handler.tick(BlockHandler.Tick(plate, instance, pos))

        Assertions.assertEquals("false", instance.getBlock(pos).getProperty("powered"))
    }

    @Test
    fun unpoweredPressurePlateStaysOff() {
        val handler = handlerFor("minecraft:stone_pressure_plate")
        val pos = Pos(8.0, 64.0, 8.0)
        val plate = Block.STONE_PRESSURE_PLATE.withHandler(handler)
        instance.setBlock(pos, plate)

        handler.tick(BlockHandler.Tick(plate, instance, pos))

        Assertions.assertEquals("false", instance.getBlock(pos).getProperty("powered"))
    }

    @Test
    fun emptyWeightedPressurePlateResetsPower() {
        val handler = handlerFor("minecraft:light_weighted_pressure_plate")
        val pos = Pos(8.0, 64.0, 8.0)
        val plate = Block.LIGHT_WEIGHTED_PRESSURE_PLATE.withProperty("power", "5").withHandler(handler)
        instance.setBlock(pos, plate)

        handler.tick(BlockHandler.Tick(plate, instance, pos))

        Assertions.assertEquals("0", instance.getBlock(pos).getProperty("power"))
    }

    private fun handlerFor(key: String): BlockHandler =
        requireNotNull(MinecraftServer.getBlockManager().getHandler(key))
}
// </<@AI_UNREVIEWED>
