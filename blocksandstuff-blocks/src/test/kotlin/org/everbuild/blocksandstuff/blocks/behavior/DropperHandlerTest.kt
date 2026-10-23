package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.ItemEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.ContainerRegistrations
import org.everbuild.blocksandstuff.blocks.PlacedHandlerRegistration
import org.everbuild.blocksandstuff.blocks.redstone.vanilla.registerVanillaComponents
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHolder
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.blocksandstuff.redstone.engine.RedstoneSimulator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DropperHandlerTest {
    private lateinit var instance: Instance
    private lateinit var simulator: RedstoneSimulator
    private val base = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        ContainerRegistrations.registerDefault()
        PlacedHandlerRegistration.registerDefault()
        RedstoneRegistrations.registry.registerVanillaComponents()
        RedstoneRegistrations.enable()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        simulator = RedstoneRegistrations.install(instance)
    }

    @AfterEach
    fun teardown() {
        RedstoneRegistrations.uninstall(instance)
        MinecraftServer.stopCleanly()
    }

    private fun placeDropper(
        position: Pos = base,
        facing: String = "down",
    ): DropperHandler {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dropper")) as DropperHandler
        instance.setBlock(position, Block.DROPPER.withProperty("facing", facing).withHandler(handler))
        return handler
    }

    private fun inventoryAt(position: Pos): PhysicalInventory {
        val block = instance.getBlock(position)
        val handler =
            block.handler()
                ?: MinecraftServer.getBlockManager().getHandler(block.key().asString())
        return (handler as BlockInventoryHolder).getInventory(instance, position)
    }

    private fun placeHopper(): HopperHandler {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:hopper")) as HopperHandler
        instance.setBlock(base, Block.HOPPER.withProperty("facing", "down").withHandler(handler))
        return handler
    }

    private fun tickHopper() {
        val block = instance.getBlock(base)
        val handler = block.handler() as HopperHandler
        handler.tick(BlockHandler.Tick(block, instance, base))
    }

    @Test
    fun ejectsOneItemFromRandomFilledSlot() {
        val handler = placeDropper()
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.STONE, 3)) }

        val ejected = handler.eject(instance, base)

        Assertions.assertEquals(Material.STONE, ejected?.material())
        Assertions.assertEquals(1, ejected?.amount())
        Assertions.assertEquals(2, inventoryAt(base).getItemStack(0).amount())
        Assertions.assertTrue(
            instance.getNearbyEntities(base, 2.0)
                .filterIsInstance<ItemEntity>()
                .any { it.itemStack.material() == Material.STONE },
        )
    }

    @Test
    fun doesNothingWhenEmpty() {
        val handler = placeDropper()

        Assertions.assertNull(handler.eject(instance, base))
    }

    @Test
    fun redstonePulseEjectsOnlyOnce() {
        placeDropper()
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.STONE, 2)) }
        val source = base.add(1.0, 0.0, 0.0)
        instance.setBlock(source, Block.REDSTONE_BLOCK)

        simulator.onBlockChanged(source)
        simulator.tick()
        Assertions.assertEquals(1, inventoryAt(base).getItemStack(0).amount())

        simulator.tick()
        Assertions.assertEquals(1, inventoryAt(base).getItemStack(0).amount())

        instance.setBlock(source, Block.AIR)
        simulator.onBlockChanged(source)
        simulator.tick()
        instance.setBlock(source, Block.REDSTONE_BLOCK)
        simulator.onBlockChanged(source)
        simulator.tick()

        Assertions.assertEquals(0, inventoryAt(base).getItemStack(0).amount())
    }

    @Test
    fun opensNineSlotGui() {
        val handler = placeDropper()
        val player = mock<Player> { on { isSneaking } doReturn false }
        val captor = argumentCaptor<Inventory>()

        handler.onInteract(
            BlockHandler.Interaction(Block.DROPPER, instance, BlockFace.TOP, base, Vec.ZERO, player, PlayerHand.MAIN),
        )

        verify(player).openInventory(captor.capture())
        Assertions.assertEquals(9, captor.firstValue.size)
        Assertions.assertEquals(InventoryType.WINDOW_3X3, captor.firstValue.inventoryType)
    }

    @Test
    fun commandPlacedDropperGetsHandler() {
        instance.setBlock(base, Block.DROPPER.withProperty("facing", "down"))

        Assertions.assertTrue(instance.getBlock(base).handler() is DropperHandler)
    }

    @Test
    fun ejectsWhenPlacedWithoutHandler() {
        instance.setBlock(base, Block.DROPPER.withProperty("facing", "down"))
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.STONE, 1)) }
        val source = base.add(1.0, 0.0, 0.0)
        instance.setBlock(source, Block.REDSTONE_BLOCK)

        simulator.onBlockChanged(source)
        simulator.tick()

        Assertions.assertTrue(inventoryAt(base).getItemStack(0).isAir)
    }

    @Test
    fun hopperPushesIntoDropper() {
        placeHopper()
        val dropper = base.sub(0.0, 1.0, 0.0)
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dropper")) as DropperHandler
        instance.setBlock(dropper, Block.DROPPER.withProperty("facing", "down").withHandler(handler))
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.STONE, 1)) }

        tickHopper()

        Assertions.assertEquals(Material.STONE, inventoryAt(dropper).getItemStack(0).material())
    }

    @Test
    fun hopperPullsFromDropperWithoutHandler() {
        placeHopper()
        val dropper = base.add(0.0, 1.0, 0.0)
        instance.setBlock(dropper, Block.DROPPER.withProperty("facing", "down"))
        inventoryAt(dropper).transact { set -> set(0, ItemStack.of(Material.STONE, 1)) }

        tickHopper()

        Assertions.assertEquals(Material.STONE, inventoryAt(base).getItemStack(0).material())
    }

    @Test
    fun dropsContentsWhenDestroyed() {
        val handler = placeDropper()
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.DIAMOND, 3)) }

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
            handler.onDestroy(BlockHandler.Destroy(instance.getBlock(base), Block.AIR, instance, base))
        } finally {
            DroppedItemFactory.current = previous
        }

        Assertions.assertEquals(1, dropped.size)
        Assertions.assertEquals(Material.DIAMOND, dropped.single().material())
        Assertions.assertEquals(3, dropped.single().amount())
    }
}
