package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TallSeagrassPlacementRuleTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)
    private val rule = TallSeagrassPlacementRule(Block.TALL_SEAGRASS)

    @BeforeEach
    fun setUp() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun tearDown() {
        MinecraftServer.stopCleanly()
    }

    private fun update(block: Block): Block =
        rule.blockUpdate(
            BlockPlacementRule.UpdateState(instance, position.asBlockVec(), block, BlockFace.TOP),
        )

    private fun placeTallSeagrass() {
        instance.setBlock(position.relative(BlockFace.BOTTOM), Block.SAND)
        instance.setBlock(position, Block.TALL_SEAGRASS.withProperty("half", "lower"))
        instance.setBlock(position.relative(BlockFace.TOP), Block.TALL_SEAGRASS.withProperty("half", "upper"))
    }

    @Test
    fun removingUpperRemovesLower() {
        placeTallSeagrass()
        val lower = instance.getBlock(position)
        instance.setBlock(position.relative(BlockFace.TOP), Block.WATER)

        val result = update(lower)

        Assertions.assertEquals(Block.WATER.key(), result.key())
    }

    @Test
    fun removingLowerRemovesUpper() {
        placeTallSeagrass()
        val upper = instance.getBlock(position.relative(BlockFace.TOP))
        instance.setBlock(position, Block.WATER)

        val result = update(upper)

        Assertions.assertEquals(Block.WATER.key(), result.key())
        Assertions.assertEquals(Block.WATER.key(), instance.getBlock(position).key())
    }

    @Test
    fun breakingTallSeagrassDropsTwoSeagrass() {
        placeTallSeagrass()
        val dropped = mutableListOf<ItemStack>()
        val previous = DroppedItemFactory.current
        DroppedItemFactory.current =
            object : DroppedItemFactory {
                override fun spawn(
                    instance: Instance,
                    position: Point,
                    block: Block,
                ) = Unit

                override fun spawn(
                    instance: Instance,
                    position: Point,
                    item: ItemStack,
                ) {
                    dropped += item
                }
            }
        try {
            instance.setBlock(position.relative(BlockFace.TOP), Block.WATER)
            update(instance.getBlock(position))
        } finally {
            DroppedItemFactory.current = previous
        }

        Assertions.assertEquals(1, dropped.size)
        Assertions.assertEquals(2, dropped.single().amount())
        Assertions.assertEquals("minecraft:seagrass", dropped.single().material().key().asString())
    }

    @Test
    fun intactTallSeagrassSurvives() {
        placeTallSeagrass()

        val result = update(instance.getBlock(position))

        Assertions.assertEquals(Block.TALL_SEAGRASS.key(), result.key())
    }
}
