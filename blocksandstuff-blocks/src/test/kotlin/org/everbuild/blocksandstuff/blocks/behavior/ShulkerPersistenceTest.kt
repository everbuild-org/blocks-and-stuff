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
        val contents = requireNotNull(item.get(DataComponents.CONTAINER))
        Assertions.assertEquals(1, contents.size)
        Assertions.assertEquals(Material.DIAMOND, contents.first().material())

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
