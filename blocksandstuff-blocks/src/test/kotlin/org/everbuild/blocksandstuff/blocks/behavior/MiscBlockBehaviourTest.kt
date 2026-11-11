package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MiscBlockBehaviourTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 64.0, 8.0)
    private var previousDroppedItemFactory: DroppedItemFactory? = null

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        previousDroppedItemFactory = DroppedItemFactory.current
    }

    @AfterEach
    fun teardown() {
        previousDroppedItemFactory?.let { DroppedItemFactory.current = it }
        DroppedItemFactory.doDropItems = true
        MinecraftServer.stopCleanly()
    }

    private fun interaction(
        block: Block,
        player: Player,
    ): InteractionActivityContext =
        InteractionActivityContext(
            block,
            instance,
            BlockFace.TOP,
            position.asBlockVec(),
            position.asBlockVec(),
            player,
            PlayerHand.MAIN,
        )

    private fun randomTick(block: Block): RandomTickActivityContext =
        RandomTickActivityContext(block, instance, position.asBlockVec())

    @Test
    fun shovelFlattensGrassIntoDirtPath() {
        instance.setBlock(position, Block.GRASS_BLOCK)
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.AIR)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.IRON_SHOVEL))

        SHOVEL_RULE.evaluate(interaction(Block.GRASS_BLOCK, player))

        Assertions.assertTrue(instance.getBlock(position).compare(Block.DIRT_PATH))
    }

    @Test
    fun flintAndSteelLightsCampfire() {
        instance.setBlock(position, Block.CAMPFIRE.withProperty("lit", "false"))
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.FLINT_AND_STEEL))

        CAMPFIRE_LIGHT_RULE.evaluate(interaction(Block.CAMPFIRE.withProperty("lit", "false"), player))

        Assertions.assertEquals("true", instance.getBlock(position).getProperty("lit"))
    }

    @Test
    fun shovelExtinguishesCampfire() {
        instance.setBlock(position, Block.CAMPFIRE.withProperty("lit", "true"))
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.IRON_SHOVEL))

        CAMPFIRE_EXTINGUISH_RULE.evaluate(interaction(Block.CAMPFIRE.withProperty("lit", "true"), player))

        Assertions.assertEquals("false", instance.getBlock(position).getProperty("lit"))
    }

    @Test
    fun shearsHarvestBeehive() {
        val beehive = Block.BEEHIVE.withProperty("honey_level", "5")
        instance.setBlock(position, beehive)
        val dropped = recordDrops()
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.SHEARS))

        BEEHIVE_SHEAR_RULE.evaluate(interaction(beehive, player))

        Assertions.assertEquals("0", instance.getBlock(position).getProperty("honey_level"))
        Assertions.assertEquals(3, dropped.size)
        Assertions.assertTrue(dropped.all { it.material() == Material.HONEYCOMB })
    }

    @Test
    fun caveVinesGiveGlowBerriesWhenHarvested() {
        val vines = Block.CAVE_VINES.withProperty("berries", "true")
        instance.setBlock(position, vines)
        val dropped = recordDrops()
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.AIR)

        CAVE_VINES_HARVEST_RULE.evaluate(interaction(vines, player))

        Assertions.assertEquals("false", instance.getBlock(position).getProperty("berries"))
        Assertions.assertTrue(dropped.isNotEmpty())
        Assertions.assertTrue(dropped.all { it.material() == Material.GLOW_BERRIES })
    }

    @Test
    fun frostedIceMeltsIntoWater() {
        instance.setBlock(position, Block.FROSTED_ICE.withProperty("age", "3"))
        val context = randomTick(Block.FROSTED_ICE.withProperty("age", "3"))

        FROSTED_ICE_MELT_RULE.evaluate(context)
        context.newBlock?.let { instance.setBlock(position, it) }

        Assertions.assertTrue(instance.getBlock(position).compare(Block.WATER))
    }

    @Test
    fun respawnAnchorChargesWithGlowstone() {
        instance.setBlock(position, Block.RESPAWN_ANCHOR.withProperty("charges", "0"))
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.GLOWSTONE))

        RESPAWN_ANCHOR_RULE.evaluate(interaction(Block.RESPAWN_ANCHOR.withProperty("charges", "0"), player))

        Assertions.assertEquals("1", instance.getBlock(position).getProperty("charges"))
    }

    @Test
    fun respawnAnchorSetsSpawnInNether() {
        val nether =
            MinecraftServer
                .getInstanceManager()
                .createInstanceContainer(net.minestom.server.world.DimensionType.THE_NETHER)
        nether.loadChunk(0, 0).join()
        val anchor = Block.RESPAWN_ANCHOR.withProperty("charges", "2")
        nether.setBlock(position, anchor)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.AIR)

        RESPAWN_ANCHOR_RULE.evaluate(
            InteractionActivityContext(
                anchor,
                nether,
                BlockFace.TOP,
                position.asBlockVec(),
                position.asBlockVec(),
                player,
                PlayerHand.MAIN,
            ),
        )

        verify(player).setRespawnPoint(any())
    }

    @Test
    fun respawnAnchorExplodesInOverworld() {
        val anchor = Block.RESPAWN_ANCHOR.withProperty("charges", "4")
        instance.setBlock(position, anchor)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.GLOWSTONE))

        RESPAWN_ANCHOR_RULE.evaluate(interaction(anchor.withProperty("charges", "4"), player))

        Assertions.assertTrue(instance.getBlock(position).air())
    }

    @Test
    fun bedSetsSpawnAtNight() {
        instance.time = 15000
        val bed = Block.RED_BED.withProperty("part", "foot").withProperty("facing", "north")
        instance.setBlock(position, bed)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.AIR)

        BED_RULE.evaluate(interaction(bed, player))

        verify(player).setRespawnPoint(any())
    }

    @Test
    fun bedDoesNotSetSpawnDuringDay() {
        instance.time = 1000
        val bed = Block.RED_BED.withProperty("part", "foot").withProperty("facing", "north")
        instance.setBlock(position, bed)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.AIR)

        BED_RULE.evaluate(interaction(bed, player))

        verify(player, org.mockito.kotlin.never()).setRespawnPoint(any())
    }

    @Test
    fun decoratedPotStoresAndReturnsItem() {
        instance.setBlock(position, Block.DECORATED_POT)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.STONE, 3))

        DECORATED_POT_RULE.evaluate(interaction(Block.DECORATED_POT, player))

        val stored = instance.getBlock(position).getTag(Tag.ItemStack("item"))
        Assertions.assertNotNull(stored)
        Assertions.assertEquals(Material.STONE, stored!!.material())
    }

    @Test
    fun dragonEggTeleportsWhenInteracted() {
        instance.setBlock(position, Block.DRAGON_EGG)
        val player = mock<Player>()

        DRAGON_EGG_TELEPORT_RULE.evaluate(interaction(Block.DRAGON_EGG, player))

        Assertions.assertFalse(instance.getBlock(position).compare(Block.DRAGON_EGG))
    }

    @Test
    fun lodestoneBindsCompass() {
        instance.setBlock(position, Block.LODESTONE)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.COMPASS))

        LODESTONE_RULE.evaluate(interaction(Block.LODESTONE, player))

        verify(player).setItemInMainHand(any())
    }

    @Test
    fun beaconOpensInventoryWithValidBase() {
        for (x in -1..1) {
            for (z in -1..1) {
                instance.setBlock(position.add(x.toDouble(), -1.0, z.toDouble()), Block.IRON_BLOCK)
            }
        }
        instance.setBlock(position, Block.BEACON)
        val player = mock<Player>()

        BEACON_RULE.evaluate(interaction(Block.BEACON, player))

        Assertions.assertEquals(1, beaconLevel(instance, position.asBlockVec()))
        val captor = argumentCaptor<net.minestom.server.inventory.Inventory>()
        verify(player).openInventory(captor.capture())
        Assertions.assertEquals(1, (captor.firstValue as BeaconMenu).level)
    }

    @Test
    fun newRecordReplacesStoredRecord() {
        val oldRecord = ItemStack.of(Material.MUSIC_DISC_CAT)
        val newRecord = ItemStack.of(Material.MUSIC_DISC_BLOCKS)
        val jukebox =
            Block.JUKEBOX
                .withTag(Tag.ItemStack("RecordItem"), oldRecord)
                .withProperty("has_record", "true")
        instance.setBlock(position, jukebox)
        val dropped = recordDrops()
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(newRecord)

        JUKEBOX_RULE.evaluate(interaction(jukebox, player))

        val stored = instance.getBlock(position).getTag(Tag.ItemStack("RecordItem"))
        Assertions.assertEquals(Material.MUSIC_DISC_BLOCKS, stored?.material())
        Assertions.assertTrue(dropped.any { it.material() == Material.MUSIC_DISC_CAT })
    }

    @Test
    fun breakingJukeboxDropsStoredRecord() {
        val record = ItemStack.of(Material.MUSIC_DISC_CAT)
        val jukebox =
            Block.JUKEBOX
                .withTag(Tag.ItemStack("RecordItem"), record)
                .withProperty("has_record", "true")
        instance.setBlock(position, jukebox)
        val dropped = recordDrops()

        JUKEBOX_DESTROY_RULE.evaluate(
            org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext(
                jukebox,
                Block.AIR,
                instance,
                position.asBlockVec(),
            ),
        )

        Assertions.assertEquals(1, dropped.size)
        Assertions.assertEquals(Material.MUSIC_DISC_CAT, dropped.single().material())
    }

    @Test
    fun silkTouchIceDropsBlock() {
        val ice = Block.ICE
        instance.setBlock(position, ice)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(
            ItemStack.of(Material.DIAMOND_PICKAXE).with(
                net.minestom.server.component.DataComponents.ENCHANTMENTS,
                net.minestom.server.item.component.EnchantmentList(
                    net.minestom.server.registry.RegistryKey.of("silk_touch"),
                    1,
                ),
            ),
        )
        val dropped = recordDrops()

        ICE_DESTROY_RULE.evaluate(
            org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext(
                ice,
                Block.AIR,
                instance,
                position.asBlockVec(),
                player,
            ),
        )

        Assertions.assertTrue(dropped.any { it.material() == Material.ICE })
    }

    @Test
    fun packedIceDoesNotDropWhenBroken() {
        val packedIce = Block.PACKED_ICE
        instance.setBlock(position, packedIce)
        val dropped = recordDrops()

        ICE_DESTROY_RULE.evaluate(
            org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext(
                packedIce,
                Block.AIR,
                instance,
                position.asBlockVec(),
            ),
        )

        Assertions.assertTrue(dropped.isEmpty(), "packed ice must not drop itself without silk touch")
    }

    @Test
    fun goldenAxeRemovesWaxFromCopperGrate() {
        val waxed = Block.WAXED_COPPER_GRATE
        instance.setBlock(position, waxed)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.GOLDEN_AXE))

        COPPER_SCRAPING_RULE.evaluate(interaction(waxed, player))

        Assertions.assertEquals(Block.COPPER_GRATE.key(), instance.getBlock(position).key())
    }

    @Test
    fun axeScrapesOxidizedCopperGrate() {
        val oxidized = Block.OXIDIZED_COPPER_GRATE
        instance.setBlock(position, oxidized)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.GOLDEN_AXE))

        COPPER_SCRAPING_RULE.evaluate(interaction(oxidized, player))

        Assertions.assertEquals(Block.WEATHERED_COPPER_GRATE.key(), instance.getBlock(position).key())
    }

    @Test
    fun copperDoorHalvesOxidizeTogether() {
        val lower = Block.COPPER_DOOR.withProperty("half", "lower")
        val upper = Block.COPPER_DOOR.withProperty("half", "upper")
        instance.setBlock(position, lower)
        instance.setBlock(position.add(0.0, 1.0, 0.0), upper)

        for (attempt in 0 until 5000) {
            val context = RandomTickActivityContext(instance.getBlock(position), instance, position.asBlockVec())
            COPPER_OXIDATION_RULE.evaluate(context)
            val next = context.newBlock ?: continue
            instance.setBlock(position, next)
            if (next.key() == Block.EXPOSED_COPPER_DOOR.key()) break
        }

        Assertions.assertEquals(Block.EXPOSED_COPPER_DOOR.key(), instance.getBlock(position).key())
        Assertions.assertEquals(
            Block.EXPOSED_COPPER_DOOR.key(),
            instance.getBlock(position.add(0.0, 1.0, 0.0)).key(),
            "the upper door half must oxidize together with the lower half",
        )
    }

    @Test
    fun secondPlayerCannotUseSameBed() {
        val first = mock<Player>()
        val second = mock<Player>()
        val bedKey = position.asBlockVec()

        Assertions.assertTrue(BedSleepTracker.trySleep(instance, bedKey, first))
        Assertions.assertFalse(BedSleepTracker.trySleep(instance, bedKey, second))

        BedSleepTracker.remove(first)
    }

    @Test
    fun explosionPrimesNearbyTnt() {
        instance.setBlock(position.add(2.0, 0.0, 0.0), Block.TNT)

        org.everbuild.blocksandstuff.blocks.util.explodeBlocks(instance, position.asBlockVec(), 4)

        Assertions.assertTrue(
            instance.entities.any { it is org.everbuild.blocksandstuff.blocks.util.PrimedTnt },
        )
    }

    @Test
    fun flintAndSteelPlacesFireOnTopOfBlock() {
        instance.setBlock(position, Block.STONE)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.FLINT_AND_STEEL))

        val ignited = FireStarterListener.tryIgnite(instance, player, Block.STONE, position.asBlockVec(), BlockFace.TOP)

        Assertions.assertTrue(ignited)
        Assertions.assertTrue(instance.getBlock(position.add(0.0, 1.0, 0.0)).compare(Block.FIRE))
    }

    @Test
    fun flintAndSteelPlacesFireOnSide() {
        instance.setBlock(position, Block.STONE)
        instance.setBlock(position.add(1.0, -1.0, 0.0), Block.STONE)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.FLINT_AND_STEEL))

        val ignited = FireStarterListener.tryIgnite(instance, player, Block.STONE, position.asBlockVec(), BlockFace.EAST)

        Assertions.assertTrue(ignited)
        Assertions.assertTrue(instance.getBlock(position.add(1.0, 0.0, 0.0)).compare(Block.FIRE))
    }

    @Test
    fun flintAndSteelPlacesFireOnFlammableSideInMidair() {
        instance.setBlock(position, Block.OAK_PLANKS)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.FLINT_AND_STEEL))

        val ignited = FireStarterListener.tryIgnite(instance, player, Block.OAK_PLANKS, position.asBlockVec(), BlockFace.EAST)

        Assertions.assertTrue(ignited)
        Assertions.assertTrue(instance.getBlock(position.add(1.0, 0.0, 0.0)).compare(Block.FIRE))
    }

    @Test
    fun flintAndSteelDoesNotPlaceFireInMidair() {
        instance.setBlock(position, Block.STONE)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.FLINT_AND_STEEL))

        val ignited = FireStarterListener.tryIgnite(instance, player, Block.STONE, position.asBlockVec(), BlockFace.EAST)

        Assertions.assertFalse(ignited)
        Assertions.assertTrue(instance.getBlock(position.add(1.0, 0.0, 0.0)).air())
    }

    @Test
    fun flintAndSteelEventPlacesSideFire() {
        FireStarterListener.enable()
        instance.setBlock(position, Block.STONE)
        instance.setBlock(position.add(1.0, -1.0, 0.0), Block.STONE)
        val player = mock<Player>()
        whenever(player.itemInMainHand).thenReturn(ItemStack.of(Material.FLINT_AND_STEEL))

        net.minestom.server.event.EventDispatcher.call(
            net.minestom.server.event.player.PlayerBlockInteractEvent(
                player,
                PlayerHand.MAIN,
                instance,
                Block.STONE,
                position.asBlockVec(),
                position.asVec(),
                BlockFace.EAST,
            ),
        )

        Assertions.assertTrue(instance.getBlock(position.add(1.0, 0.0, 0.0)).compare(Block.FIRE))
    }

    @Test
    fun fireSpreadsOnFlammableFloor() {
        for (dx in -3..3) {
            for (dz in -3..3) {
                instance.setBlock(position.add(dx.toDouble(), 0.0, dz.toDouble()), Block.OAK_PLANKS)
            }
        }
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.FIRE.withProperty("age", "0"))

        var fireCount = 1
        repeat(2000) {
            val context = TickActivityContext(Block.FIRE, instance, position.add(0.0, 1.0, 0.0).asBlockVec())
            FIRE_TICK_RULE.evaluate(context)
            if (instance.getBlock(position.add(0.0, 1.0, 0.0)).compare(Block.FIRE)) {
                instance.setBlock(position.add(0.0, 1.0, 0.0), Block.FIRE.withProperty("age", "0"))
            }
            fireCount = countFire()
            if (fireCount > 1) return@repeat
        }

        Assertions.assertTrue(fireCount > 1, "fire did not spread")
    }

    private fun countFire(): Int {
        var count = 0
        for (dx in -3..3) {
            for (dy in -1..3) {
                for (dz in -3..3) {
                    if (instance.getBlock(position.add(dx.toDouble(), dy.toDouble(), dz.toDouble())).compare(Block.FIRE)) count++
                }
            }
        }
        return count
    }

    private fun recordDrops(): MutableList<ItemStack> {
        val dropped = mutableListOf<ItemStack>()
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
        DroppedItemFactory.doDropItems = true
        return dropped
    }
}
