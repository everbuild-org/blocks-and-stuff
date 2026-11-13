package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.blocks.util.readBlockEntityItems
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class NewBehavioursTest {
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

    private fun player(
        item: Material = Material.AIR,
        gameMode: net.minestom.server.entity.GameMode = net.minestom.server.entity.GameMode.CREATIVE,
    ): Player =
        mock {
            on { this.gameMode } doReturn gameMode
            on { itemInMainHand } doReturn ItemStack.of(item)
            on { position } doReturn position
        }

    private fun interaction(
        block: Block,
        player: Player,
    ): InteractionActivityContext = InteractionActivityContext(block, instance, BlockFace.TOP, position.asBlockVec(), position.asBlockVec(), player, PlayerHand.MAIN)

    @Test
    fun pumpkinCarvingRespectsPlayerFacing() {
        instance.setBlock(position, Block.PUMPKIN)
        val player = player(Material.SHEARS)

        PUMPKIN_CARVE_RULE.evaluate(interaction(Block.PUMPKIN, player))

        Assertions.assertEquals("north", instance.getBlock(position).getProperty("facing"))
    }

    @Test
    fun endPortalFrameAcceptsEyeOfEnder() {
        instance.setBlock(position, Block.END_PORTAL_FRAME)
        val player = player(Material.ENDER_EYE)

        END_PORTAL_FRAME_RULE.evaluate(interaction(Block.END_PORTAL_FRAME, player))

        Assertions.assertEquals("true", instance.getBlock(position).getProperty("eye"))
    }

    @Test
    fun chiseledBookshelfStoresBooks() {
        instance.setBlock(position, Block.CHISELED_BOOKSHELF)
        val player = player(Material.BOOK)

        CHISELED_BOOKSHELF_RULE.evaluate(interaction(Block.CHISELED_BOOKSHELF, player))
        CHISELED_BOOKSHELF_RULE.evaluate(interaction(Block.CHISELED_BOOKSHELF, player))

        val block = instance.getBlock(position)
        Assertions.assertEquals("true", block.getProperty("slot_0_occupied"))
        Assertions.assertEquals("true", block.getProperty("slot_1_occupied"))
        Assertions.assertEquals(2, block.readBlockEntityItemCount())
    }

    @Test
    fun chiseledBookshelfStoresEnchantedBooks() {
        instance.setBlock(position, Block.CHISELED_BOOKSHELF)
        val book =
            ItemStack.of(Material.ENCHANTED_BOOK).with(
                net.minestom.server.component.DataComponents.STORED_ENCHANTMENTS,
                net.minestom.server.item.component.EnchantmentList(
                    net.minestom.server.registry.RegistryKey.of("silk_touch"),
                    1,
                ),
            )
        val player = mock<Player> {
            on { gameMode } doReturn net.minestom.server.entity.GameMode.CREATIVE
            on { itemInMainHand } doReturn book
            on { position } doReturn position
        }

        CHISELED_BOOKSHELF_RULE.evaluate(interaction(Block.CHISELED_BOOKSHELF, player))

        Assertions.assertEquals(1, instance.getBlock(position).readBlockEntityItemCount())
    }

    @Test
    fun shelfStoresAndReturnsItems() {
        instance.setBlock(position, Block.OAK_SHELF)
        val player = player(Material.STONE)

        SHELF_RULE.evaluate(interaction(Block.OAK_SHELF, player))
        Assertions.assertEquals(1, instance.getBlock(position).readBlockEntityItemCount())

        SHELF_RULE.evaluate(interaction(Block.OAK_SHELF, player(  Material.AIR)))
        Assertions.assertEquals(0, instance.getBlock(position).readBlockEntityItemCount())
    }

    @Test
    fun potentSulfurOverLavaBecomesContinuous() {
        val handler = PotentSulfurHandler()
        instance.setBlock(position, Block.POTENT_SULFUR.withHandler(handler))
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.LAVA)
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.WATER)

        handler.tick(BlockHandler.Tick(instance.getBlock(position), instance, position.asBlockVec()))

        Assertions.assertEquals("continuous", instance.getBlock(position).getProperty("potent_sulfur_state"))
    }

    @Test
    fun potentSulfurOverMagmaCycles() {
        val handler = PotentSulfurHandler()
        instance.setBlock(position, Block.POTENT_SULFUR.withHandler(handler))
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.MAGMA_BLOCK)
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.WATER)

        var sawErupting = false
        var stopped = false
        repeat(2000) {
            handler.tick(BlockHandler.Tick(instance.getBlock(position), instance, position.asBlockVec()))
            val state = instance.getBlock(position).getProperty("potent_sulfur_state")
            if (state == "erupting") sawErupting = true
            if (sawErupting && state == "dormant") stopped = true
        }

        Assertions.assertTrue(sawErupting, "geyser should erupt")
        Assertions.assertTrue(stopped, "geyser should return to dormant")
    }

    @Test
    fun dragonEggBreakTeleports() {
        instance.setBlock(position, Block.DRAGON_EGG)

        DRAGON_EGG_BREAK_TELEPORT_RULE.evaluate(
            DestroyActivityContext(
                Block.DRAGON_EGG,
                Block.AIR,
                instance,
                position.asBlockVec(),
                player(gameMode = net.minestom.server.entity.GameMode.SURVIVAL),
            ),
        )

        Assertions.assertTrue(instance.getBlock(position).air())
        val fell =
            instance.getNearbyEntities(position, 10.0).any {
                it is org.everbuild.blocksandstuff.blocks.util.FallingBlock && it.block.compare(Block.DRAGON_EGG)
            }
        var found = fell
        for (dx in -7..7) {
            for (dy in -3..3) {
                for (dz in -7..7) {
                    if (instance.getBlock(position.add(dx.toDouble(), dy.toDouble(), dz.toDouble())).compare(Block.DRAGON_EGG)) found = true
                }
            }
        }
        Assertions.assertTrue(found, "dragon egg should teleport nearby")
    }

    @Test
    fun dragonEggFallsWhenSupportRemoved() {
        org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations.registerDefault()
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.STONE)
        instance.setBlock(position, Block.DRAGON_EGG)

        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.AIR)

        Assertions.assertTrue(instance.getBlock(position).air(), "dragon egg should start falling")
    }

    @Test
    fun bonemealNyliumCreatesPatch() {
        for (dx in -3..3) {
            for (dz in -3..3) {
                instance.setBlock(position.add(dx.toDouble(), -1.0, dz.toDouble()), Block.NETHERRACK)
            }
        }
        instance.setBlock(position, Block.CRIMSON_NYLIUM)

        NYLIUM_BONE_MEAL_RULE.evaluate(interaction(Block.CRIMSON_NYLIUM, player(Material.BONE_MEAL)))

        var flora = 0
        for (dx in -3..3) {
            for (dy in -1..2) {
                for (dz in -3..3) {
                    val block = instance.getBlock(position.add(dx.toDouble(), dy.toDouble(), dz.toDouble()))
                    if (block.compare(Block.CRIMSON_ROOTS) || block.compare(Block.CRIMSON_FUNGUS) || block.compare(Block.NETHER_SPROUTS)) flora++
                }
            }
        }
        Assertions.assertTrue(flora > 0, "expected a crimson flora patch")
    }

    @Test
    fun bonemealMossSpreads() {
        for (dx in -3..3) {
            for (dz in -3..3) {
                instance.setBlock(position.add(dx.toDouble(), -1.0, dz.toDouble()), Block.STONE)
            }
        }
        instance.setBlock(position, Block.MOSS_BLOCK)

        MOSS_BONE_MEAL_RULE.evaluate(interaction(Block.MOSS_BLOCK, player(Material.BONE_MEAL)))

        val spread =
            (-3..3).sumOf { dx ->
                (-3..3).count { dz -> instance.getBlock(position.add(dx.toDouble(), -1.0, dz.toDouble())).compare(Block.MOSS_BLOCK) }
            }
        Assertions.assertTrue(spread > 0, "expected moss to spread")
    }

    @Test
    fun bonemealRedShrubSpreadsShrubs() {
        for (dx in -3..3) {
            for (dz in -3..3) {
                instance.setBlock(position.add(dx.toDouble(), -1.0, dz.toDouble()), Block.SAND)
            }
        }
        instance.setBlock(position, Block.RED_SHRUB)

        RED_SHRUB_BONE_MEAL_RULE.evaluate(interaction(Block.RED_SHRUB, player(Material.BONE_MEAL)))

        val shrubs =
            (-3..3).sumOf { dx ->
                (-3..3).count { dz -> instance.getBlock(position.add(dx.toDouble(), 0.0, dz.toDouble())).compare(Block.RED_SHRUB) }
            }
        Assertions.assertTrue(shrubs > 1, "expected additional red shrubs")
    }

    @Test
    fun fungusGrowsHugeFungus() {
        instance.setBlock(position, Block.CRIMSON_FUNGUS)
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.CRIMSON_NYLIUM)
        val player = player(Material.BONE_MEAL)

        repeat(100) {
            FUNGUS_GROWTH_RULE.evaluate(interaction(instance.getBlock(position), player))
            if (instance.getBlock(position).compare(Block.CRIMSON_STEM)) return@repeat
        }

        Assertions.assertTrue(instance.getBlock(position).compare(Block.CRIMSON_STEM), "expected a huge crimson fungus stem")
    }

    @Test
    fun fungusDoesNotGrowOnDirt() {
        instance.setBlock(position, Block.CRIMSON_FUNGUS)
        instance.setBlock(position.add(0.0, -1.0, 0.0), Block.GRASS_BLOCK)
        val player = player(Material.BONE_MEAL)

        repeat(100) {
            FUNGUS_GROWTH_RULE.evaluate(interaction(instance.getBlock(position), player))
        }

        Assertions.assertTrue(instance.getBlock(position).compare(Block.CRIMSON_FUNGUS))
    }

    private fun Block.readBlockEntityItemCount(): Int = readBlockEntityItems().size
}
