package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.color.DyeColor
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

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
        cushion.setInstance(instance, position.add(0.5, 0.5, 0.5)).join()

        repeat(101) { cushion.tick(it.toLong()) }

        Assertions.assertFalse(cushion.isRemoved)
    }

    @Test
    fun cushionPlacementUsesBottomSlabSurfaceHeight() {
        instance.setBlock(position, Block.STONE_SLAB.withProperty("type", "bottom"))

        val spawn =
            CushionBehaviour.placementPosition(
                instance,
                position,
                Vec(0.5, 0.5, 0.5),
                BlockFace.TOP,
            )

        Assertions.assertNotNull(spawn)
        Assertions.assertEquals(64.5, spawn!!.y())
        Assertions.assertEquals(spawn, CushionBehaviour.findFreePosition(instance, spawn))
        Assertions.assertTrue(CushionBehaviour.supports(instance, spawn))

        val cushion = CushionEntity(DyeColor.BLUE)
        cushion.setInstance(instance, spawn).join()
        instance.setBlock(position, Block.AIR)
        repeat(101) { cushion.tick(it.toLong()) }

        Assertions.assertTrue(cushion.isRemoved)
    }

    @Test
    fun cushionPlacementRequiresTheTracedTopSurfaceHeight() {
        instance.setBlock(position, Block.STONE_SLAB.withProperty("type", "bottom"))

        val spawn =
            CushionBehaviour.placementPosition(
                instance,
                position,
                Vec(0.5, 0.75, 0.5),
                BlockFace.TOP,
            )

        Assertions.assertNull(spawn)
    }

    @Test
    fun cushionCanBePlacedOnAnvilTop() {
        instance.setBlock(position, Block.ANVIL)

        val spawn =
            CushionBehaviour.placementPosition(
                instance,
                position,
                Vec(0.5, 1.0, 0.5),
                BlockFace.TOP,
            )

        Assertions.assertNotNull(spawn)
        Assertions.assertEquals(65.0, spawn!!.y())
        Assertions.assertEquals(spawn, CushionBehaviour.findFreePosition(instance, spawn, position))
        Assertions.assertTrue(CushionBehaviour.supports(instance, spawn))
    }

    @Test
    fun cushionUseEventPlacesOnAnvilTop() {
        instance.setBlock(position, Block.ANVIL)
        CushionBehaviour.enable()
        val cushionItem = ItemStack.of(requireNotNull(Material.fromKey("minecraft:black_cushion")))
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.SURVIVAL
                on { instance } doReturn instance
                on { getItemInHand(PlayerHand.MAIN) } doReturn cushionItem
            }

        EventDispatcher.call(
            PlayerUseItemOnBlockEvent(
                player,
                PlayerHand.MAIN,
                cushionItem,
                position,
                Vec(0.5, 1.0, 0.5),
                BlockFace.TOP,
            ),
        )

        verify(player).setItemInHand(PlayerHand.MAIN, ItemStack.AIR)
        val cushion = instance.entities.filterIsInstance<CushionEntity>().single()
        Assertions.assertEquals(65.0, cushion.position.y())
    }

    @Test
    fun cushionStaysOnTheLowerTreadOfStairs() {
        instance.setBlock(
            position,
            Block.BIRCH_STAIRS
                .withProperty("facing", "north")
                .withProperty("half", "bottom")
                .withProperty("shape", "straight"),
        )
        val lowerTreadCursor =
            listOf(
                Vec(0.25, 0.5, 0.25),
                Vec(0.75, 0.5, 0.25),
                Vec(0.25, 0.5, 0.75),
                Vec(0.75, 0.5, 0.75),
            ).firstNotNullOfOrNull { cursor ->
                CushionBehaviour
                    .placementPosition(instance, position, cursor, BlockFace.TOP)
                    ?.takeIf { it.y() == position.y() + 0.5 }
            }

        Assertions.assertNotNull(lowerTreadCursor, "expected a hit point on the lower stair tread")
        val spawn = requireNotNull(lowerTreadCursor)
        val placeable = CushionBehaviour.findFreePosition(instance, spawn, position)

        Assertions.assertEquals(spawn.y(), placeable?.y())
        Assertions.assertTrue(CushionBehaviour.supports(instance, spawn))
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
