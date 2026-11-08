package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
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

    @Test
    fun clusterDestroyDropsFourShards() {
        val dropped = recordDrops(Block.AMETHYST_CLUSTER)

        Assertions.assertEquals(1, dropped.size)
        Assertions.assertEquals(Material.AMETHYST_SHARD, dropped.single().material())
        Assertions.assertEquals(4, dropped.single().amount())
    }

    @Test
    fun budDestroyDropsItself() {
        val dropped = recordDrops(Block.LARGE_AMETHYST_BUD)

        Assertions.assertEquals(1, dropped.size)
        Assertions.assertEquals("minecraft:large_amethyst_bud", dropped.single().material().key().asString())
    }

    private fun recordDrops(block: Block): List<ItemStack> {
        val dropped = mutableListOf<ItemStack>()
        val previous = DroppedItemFactory.current
        DroppedItemFactory.current =
            object : DroppedItemFactory {
                override fun spawn(
                    instance: Instance,
                    position: Point,
                    block: Block,
                ) {
                    block.material()?.let { dropped += ItemStack.of(it) }
                }

                override fun spawn(
                    instance: Instance,
                    position: Point,
                    item: ItemStack,
                ) {
                    dropped += item
                }
            }
        try {
            AMETHYST_DROP_RULE.evaluate(
                DestroyActivityContext(block, Block.AIR, instance, position.asBlockVec(), player = mock<Player>()),
            )
        } finally {
            DroppedItemFactory.current = previous
        }
        return dropped
    }
}
