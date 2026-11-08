package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class BuddingAmethystBehaviourTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun context(): RandomTickActivityContext =
        RandomTickActivityContext(Block.BUDDING_AMETHYST, instance, position.asBlockVec())

    private fun firstFaceRandom(): Random =
        object : Random() {
            override fun nextInt(bound: Int): Int = 0
        }

    private fun neighbours(): List<Pair<BlockFace, Block>> =
        BlockFace.entries.map { it to instance.getBlock(position.relative(it)) }

    @Test
    fun growsSmallBudOnEmptyNeighbour() {
        val rule = BuddingAmethystBehaviour(firstFaceRandom()).rule("test:amethyst")
        instance.setBlock(position, Block.BUDDING_AMETHYST)

        rule.evaluate(context())

        val buds = neighbours().count { (_, block) -> block.compare(Block.SMALL_AMETHYST_BUD) }
        Assertions.assertEquals(1, buds)
    }

    @Test
    fun advancesBudStage() {
        val rule = BuddingAmethystBehaviour(firstFaceRandom()).rule("test:amethyst")
        instance.setBlock(position, Block.BUDDING_AMETHYST)
        BlockFace.entries.forEach { face ->
            instance.setBlock(position.relative(face), Block.SMALL_AMETHYST_BUD)
        }

        rule.evaluate(context())

        val medium = neighbours().count { (_, block) -> block.compare(Block.MEDIUM_AMETHYST_BUD) }
        Assertions.assertEquals(1, medium)
    }

    @Test
    fun growsWaterloggedBudIntoWater() {
        val rule = BuddingAmethystBehaviour(firstFaceRandom()).rule("test:amethyst")
        instance.setBlock(position, Block.BUDDING_AMETHYST)
        BlockFace.entries.forEach { face ->
            instance.setBlock(position.relative(face), Block.WATER)
        }

        rule.evaluate(context())

        val buds =
            neighbours().filter { (_, block) -> block.compare(Block.SMALL_AMETHYST_BUD) }
        Assertions.assertEquals(1, buds.size)
        Assertions.assertEquals("true", buds.single().second.getProperty("waterlogged"))
    }

    @Test
    fun doesNothingWhenAllNeighboursAreFull() {
        val rule = BuddingAmethystBehaviour(firstFaceRandom()).rule("test:amethyst")
        instance.setBlock(position, Block.BUDDING_AMETHYST)
        BlockFace.entries.forEach { face ->
            instance.setBlock(position.relative(face), Block.STONE)
        }

        rule.evaluate(context())

        val buds = neighbours().count { (_, block) -> block.key().asString().contains("amethyst") }
        Assertions.assertEquals(0, buds)
    }
}
