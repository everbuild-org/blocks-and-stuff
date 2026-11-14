package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.blocks.ContainerRegistrations
import org.everbuild.blocksandstuff.recipes.smelting.FurnaceArchetype
import org.everbuild.blocksandstuff.recipes.smelting.furnace.FurnaceHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HopperRollbackRegressionTest {
    private lateinit var instance: InstanceContainer

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        ContainerRegistrations.registerDefault()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun fullHopperMustLeaveRejectedFurnaceOutputInItsOriginalSlot() {
        val hopperPos = Vec(8.0, 64.0, 8.0)
        val furnacePos = hopperPos.add(0.0, 1.0, 0.0)
        val hopper = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:hopper")) as HopperHandler
        val furnace = FurnaceHandler()
        instance.setBlock(hopperPos, Block.HOPPER.withProperty("facing", "down").withHandler(hopper), false)
        instance.setBlock(furnacePos, Block.FURNACE.withHandler(furnace), false)

        val hopperInventory = hopper.getInventory(instance, hopperPos)
        hopperInventory.transact { set ->
            repeat(5) { set(it, ItemStack.of(Material.COBBLESTONE, 64)) }
        }
        val furnaceInventory = furnace.getInventory(instance, furnacePos)
        furnaceInventory.transact { set ->
            set(FurnaceArchetype.SLOT_INPUT, ItemStack.of(Material.RAW_IRON, 64))
            set(FurnaceArchetype.SLOT_FUEL, ItemStack.of(Material.COAL, 64))
            set(FurnaceArchetype.SLOT_OUTPUT, ItemStack.of(Material.IRON_INGOT))
        }
        val furnaceBefore = List(3) { furnaceInventory.getItemStack(it) }
        val hopperBefore = List(5) { hopperInventory.getItemStack(it) }

        // No target container below the hopper: pushing cannot free a slot before extraction.
        // Tick only the hopper, keeping furnace processing and redstone scheduling out of this test.
        hopper.tick(BlockHandler.Tick(instance.getBlock(hopperPos), instance, hopperPos))

        assertEquals(furnaceBefore, List(3) { furnaceInventory.getItemStack(it) }, "failed extraction must be lossless")
        assertEquals(hopperBefore, List(5) { hopperInventory.getItemStack(it) })
    }
}
