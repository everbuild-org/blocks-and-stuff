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

class AttachedStemPlacementRuleTest {
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

    @Test
    fun survivesOnHydratedFarmland() {
        val stem = Block.ATTACHED_MELON_STEM.withProperty("facing", "north")
        instance.setBlock(position.sub(0.0, 1.0, 0.0), Block.FARMLAND.withProperty("moisture", "7"))
        instance.setBlock(position, stem)
        instance.setBlock(position.add(0.0, 0.0, -1.0), Block.MELON)

        val result =
            AttachedStemPlacementRule(Block.ATTACHED_MELON_STEM).blockUpdate(
                BlockPlacementRule.UpdateState(instance, position.asBlockVec(), stem, BlockFace.TOP),
            )

        Assertions.assertEquals(stem.key(), result.key())
    }

    @Test
    fun revertsToStraightStemWhenFruitRemoved() {
        val stem = Block.ATTACHED_MELON_STEM.withProperty("facing", "north")
        instance.setBlock(position.sub(0.0, 1.0, 0.0), Block.FARMLAND)
        instance.setBlock(position, stem)

        val result =
            AttachedStemPlacementRule(Block.ATTACHED_MELON_STEM).blockUpdate(
                BlockPlacementRule.UpdateState(instance, position.asBlockVec(), stem, BlockFace.TOP),
            )

        Assertions.assertEquals(Block.MELON_STEM.key(), result.key())
        Assertions.assertEquals("7", result.getProperty("age"))
    }

    @Test
    fun dropsWhenFarmlandRemoved() {
        val stem = Block.ATTACHED_MELON_STEM.withProperty("facing", "north")
        instance.setBlock(position.sub(0.0, 1.0, 0.0), Block.DIRT)
        instance.setBlock(position, stem)

        val result =
            AttachedStemPlacementRule(Block.ATTACHED_MELON_STEM).blockUpdate(
                BlockPlacementRule.UpdateState(instance, position.asBlockVec(), stem, BlockFace.TOP),
            )

        Assertions.assertEquals(Block.AIR.key(), result.key())
    }
}
