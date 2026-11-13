package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler.PlayerPlacement
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.BlockPlacementRuleRegistrations
import org.everbuild.blocksandstuff.blocks.ContainerRegistrations
import org.everbuild.blocksandstuff.blocks.util.toContainerItem
import org.everbuild.blocksandstuff.common.blockinventory.insertStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ShulkerPersistenceTest {
    private lateinit var instance: Instance
    private val source = Pos(8.0, 65.0, 8.0)
    private val target = Pos(10.0, 65.0, 10.0)
    private val archetype = ContainerArchetype(Component.translatable("container.shulkerBox"), InventoryType.SHULKER_BOX, 27)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockPlacementRuleRegistrations.registerDefault()
        ContainerRegistrations.registerDefault()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun contentsSurviveBreakAndPlace() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:shulker_box"))
        instance.setBlock(source, Block.SHULKER_BOX.withHandler(handler))
        archetype
            .Backend(source, instance)
            .getViewableInventory()
            .setItemStack(0, ItemStack.of(Material.DIAMOND, 3))

        val item = requireNotNull(instance.getBlock(source).toContainerItem())
        Assertions.assertNotNull(item.get(DataComponents.BLOCK_ENTITY_DATA))
        Assertions.assertTrue(item.get(DataComponents.CONTAINER).isNullOrEmpty(), "shulker item must not carry a non-empty container component")

        val player =
            mock<Player> {
                on { position } doReturn target
                on { isSneaking } doReturn false
                on { getItemInHand(PlayerHand.MAIN) } doReturn item
            }
        instance.placeBlock(
            PlayerPlacement(
                Block.SHULKER_BOX,
                Block.AIR,
                instance,
                target,
                player,
                PlayerHand.MAIN,
                BlockFace.TOP,
                0f,
                0f,
                0f,
            ),
        )

        val loaded = archetype.Backend(target, instance)
        Assertions.assertEquals(Material.DIAMOND, loaded.getItemStack(0).material())
        Assertions.assertEquals(3, loaded.getItemStack(0).amount())
    }

    @Test
    fun spacedContentsSurviveItemEncoding() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:shulker_box"))
        instance.setBlock(source, Block.SHULKER_BOX.withHandler(handler))
        val inventory = archetype.Backend(source, instance).getViewableInventory()
        inventory.setItemStack(0, ItemStack.of(Material.DIAMOND, 3))
        inventory.setItemStack(5, ItemStack.of(Material.STONE, 12))

        val item = requireNotNull(instance.getBlock(source).toContainerItem())
        Assertions.assertNotNull(item.get(DataComponents.BLOCK_ENTITY_DATA))
        Assertions.assertTrue(item.get(DataComponents.CONTAINER).isNullOrEmpty(), "shulker item must not carry a non-empty container component")

        val nbt = ItemStack.CODEC.encode(net.minestom.server.codec.Transcoder.NBT, item).orElseThrow()
        val decoded = ItemStack.CODEC.decode(net.minestom.server.codec.Transcoder.NBT, nbt).orElseThrow()
        Assertions.assertNotNull(decoded.get(DataComponents.BLOCK_ENTITY_DATA))

        val player =
            mock<Player> {
                on { position } doReturn target
                on { isSneaking } doReturn false
                on { getItemInHand(PlayerHand.MAIN) } doReturn decoded
            }
        instance.placeBlock(
            PlayerPlacement(
                Block.SHULKER_BOX,
                Block.AIR,
                instance,
                target,
                player,
                PlayerHand.MAIN,
                BlockFace.TOP,
                0f,
                0f,
                0f,
            ),
        )

        val loaded = archetype.Backend(target, instance)
        Assertions.assertEquals(Material.DIAMOND, loaded.getItemStack(0).material())
        Assertions.assertTrue(loaded.getItemStack(1).isAir, "slot spacing must be preserved")
        Assertions.assertEquals(Material.STONE, loaded.getItemStack(5).material())
    }

    @Test
    fun movingItemsPersistsToBlock() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:shulker_box"))
        instance.setBlock(source, Block.SHULKER_BOX.withHandler(handler))
        val inventory = archetype.Backend(source, instance).getViewableInventory()
        inventory.setItemStack(0, ItemStack.of(Material.DIAMOND, 3))

        inventory.setItemStack(5, inventory.getItemStack(0))
        inventory.setItemStack(0, ItemStack.AIR)

        val reloaded = archetype.Backend(source, instance)
        Assertions.assertTrue(reloaded.getItemStack(0).isAir)
        Assertions.assertEquals(Material.DIAMOND, reloaded.getItemStack(5).material())
    }

    @Test
    fun backendChangesReachOpenInventory() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:shulker_box"))
        instance.setBlock(source, Block.SHULKER_BOX.withHandler(handler))
        val backend = archetype.Backend(source, instance)
        val inventory = backend.getViewableInventory()

        backend.transact { set -> set(3, ItemStack.of(Material.GOLD_INGOT, 2)) }

        Assertions.assertEquals(Material.GOLD_INGOT, inventory.getItemStack(3).material())
        Assertions.assertEquals(2, inventory.getItemStack(3).amount())
    }

    @Test
    fun clickMovesItemOutOfShulker() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:shulker_box"))
        instance.setBlock(source, Block.SHULKER_BOX.withHandler(handler))
        val backend = archetype.Backend(source, instance)
        val inventory = backend.getViewableInventory()
        inventory.setItemStack(0, ItemStack.of(Material.DIAMOND, 3))
        val player =
            mock<Player> {
                on { gameMode } doReturn net.minestom.server.entity.GameMode.SURVIVAL
                on { getInventory() } doReturn net.minestom.server.inventory.PlayerInventory()
            }

        inventory.handleClick(player, net.minestom.server.inventory.click.Click.Left(0))

        Assertions.assertEquals(Material.DIAMOND, player.inventory.cursorItem.material())
        Assertions.assertEquals(3, player.inventory.cursorItem.amount())
        Assertions.assertTrue(backend.getItemStack(0).isAir)
    }

    @Test
    fun shiftClickMovesItemIntoShulker() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:shulker_box"))
        instance.setBlock(source, Block.SHULKER_BOX.withHandler(handler))
        val backend = archetype.Backend(source, instance)
        val inventory = backend.getViewableInventory()
        val player =
            mock<Player> {
                on { gameMode } doReturn net.minestom.server.entity.GameMode.SURVIVAL
                on { getInventory() } doReturn net.minestom.server.inventory.PlayerInventory()
            }
        player.inventory.setItemStack(0, ItemStack.of(Material.DIAMOND, 3))

        // Window slot 27 maps to player inventory slot 0 while the shulker is open.
        inventory.handleClick(player, net.minestom.server.inventory.click.Click.LeftShift(27))

        Assertions.assertEquals(Material.DIAMOND, inventory.getItemStack(0).material())
        Assertions.assertTrue(player.inventory.getItemStack(0).isAir)
    }

    @Test
    fun clickGuardBlocksShulkerNesting() {
        val noNesting =
            ContainerArchetype(
                Component.translatable("container.shulkerBox"),
                InventoryType.SHULKER_BOX,
                27,
                acceptsShulkerBoxes = false,
            )
        val backend = noNesting.Backend(source, instance)
        val inventory = backend.getViewableInventory()
        val player =
            mock<Player> {
                on { getInventory() } doReturn net.minestom.server.inventory.PlayerInventory()
            }

        player.inventory.setItemStack(0, ItemStack.of(Material.SHULKER_BOX))
        val shiftClick =
            net.minestom.server.event.inventory.InventoryPreClickEvent(
                inventory,
                player,
                net.minestom.server.inventory.click.Click.LeftShift(27),
            )
        Assertions.assertTrue(ShulkerClickGuard.wouldNestShulker(inventory, shiftClick))

        player.inventory.cursorItem = ItemStack.of(Material.SHULKER_BOX)
        val cursorClick =
            net.minestom.server.event.inventory.InventoryPreClickEvent(
                inventory,
                player,
                net.minestom.server.inventory.click.Click.Left(0),
            )
        Assertions.assertTrue(ShulkerClickGuard.wouldNestShulker(inventory, cursorClick))
    }

    @Test
    fun shulkerCannotContainShulkerBox() {
        val noNesting =
            ContainerArchetype(
                Component.translatable("container.shulkerBox"),
                InventoryType.SHULKER_BOX,
                27,
                acceptsShulkerBoxes = false,
            )
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:shulker_box"))
        instance.setBlock(source, Block.SHULKER_BOX.withHandler(handler))
        val backend = noNesting.Backend(source, instance)
        val inventory = backend.getViewableInventory()

        inventory.setItemStack(0, ItemStack.of(Material.SHULKER_BOX))

        Assertions.assertTrue(inventory.getItemStack(0).isAir, "shulker box must not remain in the inventory")
        Assertions.assertTrue(backend.getItemStack(0).isAir, "shulker box must not be persisted")
        Assertions.assertEquals(
            Material.SHULKER_BOX,
            backend.insertStack(ItemStack.of(Material.SHULKER_BOX)).material(),
            "hoppers must not insert shulker boxes",
        )
    }

    @Test
    fun itemLoreListsContents() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:shulker_box"))
        instance.setBlock(source, Block.SHULKER_BOX.withHandler(handler))
        val inventory = archetype.Backend(source, instance).getViewableInventory()
        inventory.setItemStack(0, ItemStack.of(Material.DIAMOND, 3))
        inventory.setItemStack(1, ItemStack.of(Material.STONE, 12))

        val item = requireNotNull(instance.getBlock(source).toContainerItem())
        val lore = requireNotNull(item.get(DataComponents.LORE))
        val text = lore.joinToString("\n") { PlainTextComponentSerializer.plainText().serialize(it) }
        val firstName = lore.first().children().first() as? TranslatableComponent

        Assertions.assertEquals(2, lore.size)
        Assertions.assertTrue(text.contains("3x"), "lore should mention the diamond count, got: $text")
        Assertions.assertTrue(text.contains("12x"), "lore should mention the stone count, got: $text")
        Assertions.assertEquals("item.minecraft.diamond", firstName?.key())
    }
}
