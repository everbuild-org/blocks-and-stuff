package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
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
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.ContainerRegistrations
import org.everbuild.blocksandstuff.blocks.redstone.vanilla.registerVanillaComponents
import org.everbuild.blocksandstuff.blocks.util.containerSignal
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHolder
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.blocksandstuff.redstone.engine.RedstoneSimulator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class HopperHandlerTest {
    private lateinit var instance: Instance
    private lateinit var simulator: RedstoneSimulator
    private val base = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        ContainerRegistrations.registerDefault()
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

    private fun handler(key: String): BlockHandler = requireNotNull(MinecraftServer.getBlockManager().getHandler(key))

    private fun placeHopper(
        position: Pos = base,
        facing: String = "down",
    ): HopperHandler {
        val handler = handler("minecraft:hopper") as HopperHandler
        instance.setBlock(position, Block.HOPPER.withProperty("facing", facing).withHandler(handler))
        return handler
    }

    private fun placeContainer(
        position: Pos,
        block: Block,
        key: String,
    ) {
        instance.setBlock(position, block.withHandler(handler(key)))
    }

    private fun inventoryAt(position: Pos): PhysicalInventory {
        val block = instance.getBlock(position)
        val handler =
            block.handler()
                ?: MinecraftServer.getBlockManager().getHandler(block.key().asString())
        return (handler as BlockInventoryHolder).getInventory(instance, position)
    }

    private fun tickHopper(position: Pos = base) {
        val block = instance.getBlock(position)
        val handler = block.handler() as HopperHandler
        handler.tick(BlockHandler.Tick(block, instance, position))
    }

    @Test
    fun opensFiveSlotInventory() {
        val handler = placeHopper()
        val player = mock<Player> { on { isSneaking } doReturn false }
        val captor = argumentCaptor<net.minestom.server.inventory.Inventory>()

        handler.onInteract(
            BlockHandler.Interaction(Block.HOPPER, instance, BlockFace.TOP, base, Vec.ZERO, player, PlayerHand.MAIN),
        )

        verify(player).openInventory(captor.capture())
        Assertions.assertEquals(5, captor.firstValue.size)
    }

    @Test
    fun inventorySurvivesHandlerRecreation() {
        placeHopper()
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.DIAMOND, 3)) }

        val freshHandler = handler("minecraft:hopper") as HopperHandler
        val fresh = freshHandler.getInventory(instance, base)

        Assertions.assertEquals(Material.DIAMOND, fresh.getItemStack(0).material())
        Assertions.assertEquals(3, fresh.getItemStack(0).amount())
    }

    @Test
    fun pushesOneItemIntoFacingContainer() {
        placeHopper(facing = "down")
        placeContainer(base.sub(0.0, 1.0, 0.0), Block.CHEST, "minecraft:chest")
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.STONE, 3)) }

        tickHopper()

        Assertions.assertEquals(2, inventoryAt(base).getItemStack(0).amount())
        Assertions.assertEquals(1, inventoryAt(base.sub(0.0, 1.0, 0.0)).getItemStack(0).amount())
    }

    @Test
    fun respectsTransferCooldown() {
        placeHopper(facing = "down")
        placeContainer(base.sub(0.0, 1.0, 0.0), Block.CHEST, "minecraft:chest")
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.STONE, 3)) }

        tickHopper()
        repeat(7) { tickHopper() }

        Assertions.assertEquals(2, inventoryAt(base).getItemStack(0).amount())
        Assertions.assertEquals(1, inventoryAt(base.sub(0.0, 1.0, 0.0)).getItemStack(0).amount())

        tickHopper()

        Assertions.assertEquals(1, inventoryAt(base).getItemStack(0).amount())
        Assertions.assertEquals(2, inventoryAt(base.sub(0.0, 1.0, 0.0)).getItemStack(0).amount())
    }

    @Test
    fun pullsOneItemFromContainerAbove() {
        placeHopper()
        placeContainer(base.add(0.0, 1.0, 0.0), Block.CHEST, "minecraft:chest")
        inventoryAt(base.add(0.0, 1.0, 0.0)).transact { set -> set(0, ItemStack.of(Material.DIAMOND, 2)) }

        tickHopper()

        Assertions.assertEquals(1, inventoryAt(base).getItemStack(0).amount())
        Assertions.assertEquals(1, inventoryAt(base.add(0.0, 1.0, 0.0)).getItemStack(0).amount())
    }

    @Test
    fun checksAcceptanceBeforeExtractingFromUnwritableSource() {
        placeHopper()
        inventoryAt(base).transact { set ->
            for (slot in 0 until 5) set(slot, ItemStack.of(Material.STONE, 64))
        }

        val source =
            mock<PhysicalInventory> {
                on { extractableSlots } doReturn listOf(0)
                on { getItemStack(0) } doReturn ItemStack.of(Material.IRON_INGOT)
            }
        val sourcePosition = base.add(0.0, 1.0, 0.0)
        val sourceHandler =
            object : BlockHandler, BlockInventoryHolder {
                override fun getKey(): Key = Key.key("test:unwritable_container")

                override fun getInventory(
                    instance: Instance,
                    blockPos: Point,
                ): PhysicalInventory = source
            }
        instance.setBlock(sourcePosition, Block.CHEST.withHandler(sourceHandler))

        tickHopper()

        verify(source, never()).transact(any())
        Assertions.assertEquals(64, inventoryAt(base).getItemStack(0).amount())
    }

    @Test
    fun hopperDoesNotTransferWhilePowered() {
        placeHopper(facing = "down")
        placeContainer(base.sub(0.0, 1.0, 0.0), Block.CHEST, "minecraft:chest")
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.STONE, 2)) }
        instance.setBlock(base.add(1.0, 0.0, 0.0), Block.REDSTONE_BLOCK)

        tickHopper()

        Assertions.assertEquals(2, inventoryAt(base).getItemStack(0).amount())
        Assertions.assertTrue(inventoryAt(base.sub(0.0, 1.0, 0.0)).getItemStack(0).isAir)

        instance.setBlock(base.add(1.0, 0.0, 0.0), Block.AIR)
        tickHopper()

        Assertions.assertEquals(1, inventoryAt(base).getItemStack(0).amount())
    }

    @Test
    fun collectsItemEntitiesAbove() {
        placeHopper()
        val entity = ItemEntity(ItemStack.of(Material.DIAMOND, 2))
        entity.setInstance(instance, Pos(base.x() + 0.5, base.y() + 1.0, base.z() + 0.5)).join()

        tickHopper()

        Assertions.assertEquals(Material.DIAMOND, inventoryAt(base).getItemStack(0).material())
        Assertions.assertEquals(2, inventoryAt(base).getItemStack(0).amount())
        Assertions.assertTrue(entity.isRemoved)
    }

    @Test
    fun dropsContentsWhenDestroyed() {
        val handler = placeHopper()
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

    @Test
    fun comparatorReadsHopperFillLevel() {
        placeHopper(facing = "east")
        val comparator = base.add(1.0, 0.0, 0.0)
        instance.setBlock(comparator, Block.COMPARATOR.withProperty("facing", "west"))

        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.STONE, 1)) }
        simulator.onBlockChanged(comparator)
        repeat(3) { simulator.tick() } // comparator delay = one redstone tick
        Assertions.assertEquals(1, simulator.weakSignal(comparator, BlockFace.EAST))

        inventoryAt(base).transact { set ->
            for (slot in 0 until 5) set(slot, ItemStack.of(Material.STONE, 64))
        }
        simulator.onBlockChanged(comparator)
        repeat(3) { simulator.tick() }
        Assertions.assertEquals(15, simulator.weakSignal(comparator, BlockFace.EAST))
    }

    @Test
    fun containerSignalUsesHopperSlotCount() {
        placeHopper()
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.STONE, 64)) }

        Assertions.assertEquals(3, containerSignal(instance, base))
    }
}
