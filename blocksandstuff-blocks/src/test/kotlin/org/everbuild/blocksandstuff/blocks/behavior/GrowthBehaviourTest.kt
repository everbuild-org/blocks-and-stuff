package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GrowthBehaviourTest {
    private lateinit var instance: Instance
    private val position = Pos(4.0, 64.0, 4.0)

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

    private fun context(block: Block): RandomTickActivityContext = RandomTickActivityContext(block, instance, position.asBlockVec())

    @Test
    fun stemGrowsFruitOnNeighbour() {
        val rule = StemGrowthBehaviour(Block.PUMPKIN, Block.ATTACHED_PUMPKIN_STEM, growthChance = 1, minLight = 0).rule("test:stem")
        instance.setBlock(position, Block.PUMPKIN_STEM.withProperty("age", "7"))
        for (offset in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            instance.setBlock(position.add(offset.first.toDouble(), -1.0, offset.second.toDouble()), Block.FARMLAND)
        }

        rule.evaluate(context(Block.PUMPKIN_STEM.withProperty("age", "7")))

        val fruits =
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).count {
                instance.getBlock(position.add(it.first.toDouble(), 0.0, it.second.toDouble())).key() == Block.PUMPKIN.key()
            }
        Assertions.assertEquals(1, fruits)
    }

    @Test
    fun melonStemGrowsFruitOnNeighbour() {
        val rule = StemGrowthBehaviour(Block.MELON, Block.ATTACHED_MELON_STEM, growthChance = 1, minLight = 0).rule("test:melon")
        instance.setBlock(position, Block.MELON_STEM.withProperty("age", "7"))
        for (offset in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            instance.setBlock(position.add(offset.first.toDouble(), -1.0, offset.second.toDouble()), Block.FARMLAND)
        }

        rule.evaluate(context(Block.MELON_STEM.withProperty("age", "7")))

        val fruits =
            listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1).count {
                instance.getBlock(position.add(it.first.toDouble(), 0.0, it.second.toDouble())).key() == Block.MELON.key()
            }
        Assertions.assertEquals(1, fruits)
        Assertions.assertEquals(Block.ATTACHED_MELON_STEM.key(), instance.getBlock(position).key())
    }

    @Test
    fun fourSpruceSaplingsAlterGroundToPodzol() {
        for (dx in -3..4) {
            for (dz in -3..4) {
                instance.setBlock(position.add(dx.toDouble(), -1.0, dz.toDouble()), Block.GRASS_BLOCK)
            }
        }
        for (dx in 0..1) {
            for (dz in 0..1) {
                instance.setBlock(position.add(dx.toDouble(), 0.0, dz.toDouble()), Block.SPRUCE_SAPLING)
            }
        }

        growSapling(instance, position, Block.SPRUCE_SAPLING, java.util.Random(1))

        val podzols =
            (-3..4).sumOf { dx ->
                (-3..4).count { dz -> instance.getBlock(position.add(dx.toDouble(), -1.0, dz.toDouble())).key() == Block.PODZOL.key() }
            }
        Assertions.assertTrue(podzols > 0, "expected the ground under the mega spruce to become podzol")
    }

    @Test
    fun verticalGrowsUpwards() {
        val rule = VerticalGrowthBehaviour(maxHeight = 3, growthChance = 1).rule("test:vertical")
        instance.setBlock(position, Block.SUGAR_CANE)

        rule.evaluate(context(Block.SUGAR_CANE))

        Assertions.assertEquals(Block.SUGAR_CANE.key(), instance.getBlock(position.add(0.0, 1.0, 0.0)).key())
    }

    @Test
    fun verticalStopsAtMaxHeight() {
        val rule = VerticalGrowthBehaviour(maxHeight = 3, growthChance = 1).rule("test:vertical")
        val top = position.add(0.0, 2.0, 0.0)
        instance.setBlock(position, Block.SUGAR_CANE)
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.SUGAR_CANE)
        instance.setBlock(top, Block.SUGAR_CANE)

        rule.evaluate(RandomTickActivityContext(Block.SUGAR_CANE, instance, top.asBlockVec()))

        Assertions.assertTrue(instance.getBlock(top.add(0.0, 1.0, 0.0)).air())
    }

    @Test
    fun saplingGrowsIntoTree() {
        val rule =
            SaplingGrowthBehaviour(
                saplingKey = Block.OAK_SAPLING.key(),
                growthChance = 1,
                minLight = 0,
            ).rule("test:sapling")
        instance.setBlock(position, Block.OAK_SAPLING)

        rule.evaluate(context(Block.OAK_SAPLING))

        Assertions.assertEquals(Block.OAK_LOG.key(), instance.getBlock(position).key())
        val hasLeaves =
            (-3..3).any { dx ->
                (-3..3).any { dz ->
                    (0..6).any { dy ->
                        instance.getBlock(position.add(dx.toDouble(), 3.0 + dy, dz.toDouble())).key() == Block.OAK_LEAVES.key()
                    }
                }
            }
        Assertions.assertTrue(hasLeaves, "expected leaves around the tree")
    }

    @Test
    fun fourJungleSaplingsGrowLargeTree() {
        for (dx in 0..1) {
            for (dz in 0..1) {
                instance.setBlock(position.add(dx.toDouble(), 0.0, dz.toDouble()), Block.JUNGLE_SAPLING)
            }
        }

        growSapling(instance, position, Block.JUNGLE_SAPLING, java.util.Random(1))

        for (dx in 0..1) {
            for (dz in 0..1) {
                Assertions.assertEquals(
                    Block.JUNGLE_LOG.key(),
                    instance.getBlock(position.add(dx.toDouble(), 0.0, dz.toDouble())).key(),
                    "expected 2x2 jungle trunk",
                )
            }
        }
    }

    @Test
    fun singleDarkOakSaplingDoesNotGrow() {
        instance.setBlock(position, Block.DARK_OAK_SAPLING)

        growSapling(instance, position, Block.DARK_OAK_SAPLING, java.util.Random(1))

        Assertions.assertEquals(Block.DARK_OAK_SAPLING.key(), instance.getBlock(position).key())
    }

    @Test
    fun singlePaleOakSaplingDoesNotGrow() {
        instance.setBlock(position, Block.PALE_OAK_SAPLING)

        growSapling(instance, position, Block.PALE_OAK_SAPLING, java.util.Random(1))

        Assertions.assertEquals(Block.PALE_OAK_SAPLING.key(), instance.getBlock(position).key())
    }

    @Test
    fun fourDarkOakSaplingsGrowLargeTree() {        for (dx in 0..1) {
            for (dz in 0..1) {
                instance.setBlock(position.add(dx.toDouble(), 0.0, dz.toDouble()), Block.DARK_OAK_SAPLING)
            }
        }

        growSapling(instance, position, Block.DARK_OAK_SAPLING, java.util.Random(1))

        for (dx in 0..1) {
            for (dz in 0..1) {
                Assertions.assertEquals(
                    Block.DARK_OAK_LOG.key(),
                    instance.getBlock(position.add(dx.toDouble(), 0.0, dz.toDouble())).key(),
                    "expected 2x2 dark oak trunk",
                )
            }
        }
    }

    @Test
    fun poplarSaplingGrowsIntoTree() {
        val rule = SaplingGrowthBehaviour(Block.POPLAR_SAPLING.key(), growthChance = 1, minLight = 0).rule("test:poplar")
        instance.setBlock(position, Block.POPLAR_SAPLING)

        rule.evaluate(context(Block.POPLAR_SAPLING))

        Assertions.assertEquals(Block.POPLAR_LOG.key(), instance.getBlock(position).key())
    }

    @Test
    fun usesCustomTreeGeneratorFromInstanceTag() {        instance.setTag(
            TREE_GENERATORS,
            mapOf(
                Block.OAK_SAPLING.key() to
                    TreeGenerator { inst, base, _ -> inst.setBlock(base, Block.DIAMOND_BLOCK) },
            ),
        )
        val rule =
            SaplingGrowthBehaviour(
                saplingKey = Block.OAK_SAPLING.key(),
                growthChance = 1,
                minLight = 0,
            ).rule("test:sapling")
        instance.setBlock(position, Block.OAK_SAPLING)

        rule.evaluate(context(Block.OAK_SAPLING))

        Assertions.assertEquals(Block.DIAMOND_BLOCK.key(), instance.getBlock(position).key())
    }
}
