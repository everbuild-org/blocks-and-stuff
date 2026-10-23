package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
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
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class DispenserHandlerTest {
    private lateinit var instance: Instance
    private lateinit var simulator: org.everbuild.blocksandstuff.redstone.engine.RedstoneSimulator
    private val base = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        ContainerRegistrations.registerDefault()
        PlacedHandlerRegistration.registerDefault()
        DispenseBehaviors.resetDefaults()
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

    private fun inventoryAt(position: Pos): PhysicalInventory {
        val block = instance.getBlock(position)
        val handler =
            block.handler()
                ?: MinecraftServer.getBlockManager().getHandler(block.key().asString())
        return (handler as BlockInventoryHolder).getInventory(instance, position)
    }

    @Test
    fun opensNineSlotGui() {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dispenser")) as DispenserHandler
        instance.setBlock(base, Block.DISPENSER.withProperty("facing", "down").withHandler(handler))
        val player = mock<Player> { on { isSneaking } doReturn false }
        val captor = argumentCaptor<Inventory>()

        handler.onInteract(
            BlockHandler.Interaction(Block.DISPENSER, instance, BlockFace.TOP, base, Vec.ZERO, player, PlayerHand.MAIN),
        )

        verify(player).openInventory(captor.capture())
        Assertions.assertEquals(9, captor.firstValue.size)
        Assertions.assertEquals(InventoryType.WINDOW_3X3, captor.firstValue.inventoryType)
    }

    @Test
    fun inventorySurvivesHandlerRecreation() {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dispenser")) as DispenserHandler
        instance.setBlock(base, Block.DISPENSER.withProperty("facing", "down").withHandler(handler))
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.DIAMOND, 3)) }

        val fresh =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dispenser")) as DispenserHandler
        val freshInventory = fresh.getInventory(instance, base)

        Assertions.assertEquals(Material.DIAMOND, freshInventory.getItemStack(0).material())
        Assertions.assertEquals(3, freshInventory.getItemStack(0).amount())
    }

    @Test
    fun dispensesWaterBucket() {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dispenser")) as DispenserHandler
        instance.setBlock(base, Block.DISPENSER.withProperty("facing", "north").withHandler(handler))
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.WATER_BUCKET)) }

        val dispensed = handler.dispense(instance, base)

        Assertions.assertEquals(Material.WATER_BUCKET, dispensed?.material())
        Assertions.assertEquals(Block.WATER.key(), instance.getBlock(base.relative(BlockFace.NORTH)).key())
        Assertions.assertEquals(Material.BUCKET, inventoryAt(base).getItemStack(0).material())
    }

    @Test
    fun dispensesLavaBucket() {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dispenser")) as DispenserHandler
        instance.setBlock(base, Block.DISPENSER.withProperty("facing", "up").withHandler(handler))
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.LAVA_BUCKET)) }

        handler.dispense(instance, base)

        Assertions.assertEquals(Block.LAVA.key(), instance.getBlock(base.add(0.0, 1.0, 0.0)).key())
        Assertions.assertEquals(Material.BUCKET, inventoryAt(base).getItemStack(0).material())
    }

    @Test
    fun picksUpPlacedWaterAgain() {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dispenser")) as DispenserHandler
        instance.setBlock(base, Block.DISPENSER.withProperty("facing", "north").withHandler(handler))
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.WATER_BUCKET)) }
        val target = base.relative(BlockFace.NORTH)

        handler.dispense(instance, base)
        handler.dispense(instance, base)

        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(target).key())
        Assertions.assertEquals(Material.WATER_BUCKET, inventoryAt(base).getItemStack(0).material())
    }

    @Test
    fun customBehaviorCanBeRegistered() {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dispenser")) as DispenserHandler
        instance.setBlock(base, Block.DISPENSER.withProperty("facing", "north").withHandler(handler))
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.DIAMOND)) }
        DispenseBehaviors.register(Material.DIAMOND) { context ->
            context.setSlot(ItemStack.of(Material.EMERALD))
            true
        }

        handler.dispense(instance, base)

        Assertions.assertEquals(Material.EMERALD, inventoryAt(base).getItemStack(0).material())
        Assertions.assertTrue(instance.getBlock(base.relative(BlockFace.NORTH)).air())
    }

    @Test
    fun customItemsCanMatchByComponents() {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dispenser")) as DispenserHandler
        instance.setBlock(base, Block.DISPENSER.withProperty("facing", "north").withHandler(handler))
        val customItem = ItemStack.of(Material.PAPER).with(DataComponents.CUSTOM_NAME, Component.text("Custom Item"))
        inventoryAt(base).transact { set -> set(0, customItem) }
        DispenseBehaviors.register({ it.get(DataComponents.CUSTOM_NAME) != null }) { context ->
            context.setSlot(ItemStack.of(Material.EMERALD))
            true
        }

        handler.dispense(instance, base)

        Assertions.assertEquals(Material.EMERALD, inventoryAt(base).getItemStack(0).material())
    }

    @Test
    fun redstonePulseDispensesWater() {
        val handler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:dispenser")) as DispenserHandler
        instance.setBlock(base, Block.DISPENSER.withProperty("facing", "north").withHandler(handler))
        inventoryAt(base).transact { set -> set(0, ItemStack.of(Material.WATER_BUCKET)) }
        val source = base.add(1.0, 0.0, 0.0)
        instance.setBlock(source, Block.REDSTONE_BLOCK)

        simulator.onBlockChanged(source)
        simulator.tick()

        Assertions.assertEquals(Block.WATER.key(), instance.getBlock(base.relative(BlockFace.NORTH)).key())
        Assertions.assertEquals(Material.BUCKET, inventoryAt(base).getItemStack(0).material())
    }

    @Test
    fun hopperPullsFromDispenserWithoutHandler() {
        val hopperHandler =
            requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:hopper")) as HopperHandler
        instance.setBlock(base, Block.HOPPER.withProperty("facing", "down").withHandler(hopperHandler))
        val dispenser = base.add(0.0, 1.0, 0.0)
        instance.setBlock(dispenser, Block.DISPENSER.withProperty("facing", "down"))
        inventoryAt(dispenser).transact { set -> set(0, ItemStack.of(Material.STONE, 1)) }

        val block = instance.getBlock(base)
        hopperHandler.tick(BlockHandler.Tick(block, instance, base))

        Assertions.assertEquals(Material.STONE, inventoryAt(base).getItemStack(0).material())
    }
}
