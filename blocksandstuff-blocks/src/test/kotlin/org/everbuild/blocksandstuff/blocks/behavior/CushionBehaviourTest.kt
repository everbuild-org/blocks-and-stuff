package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.color.DyeColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CushionBehaviourTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        DroppedItemFactory.doDropItems = false
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        DroppedItemFactory.doDropItems = true
        MinecraftServer.stopCleanly()
    }

    @Test
    fun cushionDropsWhenOverbuiltBySolidBlock() {
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
        val cushion = CushionEntity(DyeColor.BLUE)
        cushion.setInstance(instance, position.add(0.5, 0.0, 0.5)).join()
        instance.setBlock(position, Block.STONE)

        repeat(101) { cushion.tick(it.toLong()) }

        Assertions.assertTrue(cushion.isRemoved)
    }

    @Test
    fun cushionOnlyChecksEveryHundredTicks() {
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
        val cushion = CushionEntity(DyeColor.BLUE)
        cushion.setInstance(instance, position.add(0.5, 0.0, 0.5)).join()
        instance.setBlock(position, Block.STONE)

        repeat(100) { cushion.tick(it.toLong()) }
        Assertions.assertFalse(cushion.isRemoved)

        cushion.tick(100)
        Assertions.assertTrue(cushion.isRemoved)
    }

    @Test
    fun blockAboveCushionDoesNotDropIt() {
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
        val cushion = CushionEntity(DyeColor.BLUE)
        cushion.setInstance(instance, position.add(0.5, 0.0, 0.5)).join()

        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.STONE)

        repeat(101) { cushion.tick(it.toLong()) }

        Assertions.assertFalse(cushion.isRemoved)
    }

    @Test
    fun blockNextToCushionDoesNotDropIt() {
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
        val cushion = CushionEntity(DyeColor.BLUE)
        cushion.setInstance(instance, position.add(0.5, 0.0, 0.5)).join()

        instance.setBlock(position.add(1.0, 0.0, 0.0), Block.OAK_FENCE)
        instance.setBlock(position.add(1.0, 1.0, 0.0), Block.STONE)

        repeat(101) { cushion.tick(it.toLong()) }

        Assertions.assertFalse(cushion.isRemoved)
    }

    @Test
    fun blocksCanBePlacedIntoCushionCell() {
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
        val cushion = CushionEntity(DyeColor.BLUE)
        cushion.setInstance(instance, position.add(0.5, 0.0, 0.5)).join()

        Assertions.assertFalse(cushion.preventBlockPlacement())
    }

    @Test
    fun fenceInsideCushionDoesNotDropIt() {
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
        val cushion = CushionEntity(DyeColor.BLUE)
        cushion.setInstance(instance, position.add(0.5, 0.0, 0.5)).join()
        instance.setBlock(position, Block.OAK_FENCE)

        repeat(101) { cushion.tick(it.toLong()) }

        Assertions.assertFalse(cushion.isRemoved)
    }

    @Test
    fun instanceTickDropsOverbuiltCushion() {
        CushionBehaviour.enable()
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
        val cushion = CushionEntity(DyeColor.BLUE)
        cushion.setInstance(instance, position.add(0.5, 0.0, 0.5)).join()
        instance.setBlock(position, Block.STONE)

        instance.setWorldAge(99)
        instance.tick(0)

        Assertions.assertTrue(cushion.isRemoved)
    }

    @Test
    fun cushionStaysOnFence() {
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.OAK_FENCE)
        val cushion = CushionEntity(DyeColor.BLUE)
        cushion.setInstance(instance, position.add(0.5, 0.0, 0.5)).join()

        repeat(101) { cushion.tick(it.toLong()) }

        Assertions.assertFalse(cushion.isRemoved)
    }

    @Test
    fun copperGrateInsideCushionDoesNotDropIt() {
        val grates =
            listOf(
                Block.COPPER_GRATE,
                Block.EXPOSED_COPPER_GRATE,
                Block.WEATHERED_COPPER_GRATE,
                Block.OXIDIZED_COPPER_GRATE,
                Block.WAXED_COPPER_GRATE,
                Block.WAXED_EXPOSED_COPPER_GRATE,
                Block.WAXED_WEATHERED_COPPER_GRATE,
                Block.WAXED_OXIDIZED_COPPER_GRATE,
            )

        grates.forEach { grate ->
            instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
            val cushion = CushionEntity(DyeColor.BLUE)
            cushion.setInstance(instance, position.add(0.5, 0.0, 0.5)).join()
            instance.setBlock(position, grate)

            repeat(101) { cushion.tick(it.toLong()) }

            Assertions.assertFalse(cushion.isRemoved, grate.key().asString())
            cushion.remove()
            instance.setBlock(position, Block.AIR)
        }
    }
}
