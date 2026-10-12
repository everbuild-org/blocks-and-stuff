package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.listener.BlockPlacementListener
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SweetBerryBushInteractTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addSweetBerryBushes() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:sweet_berry_bush"))
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun interact(pos: Pos, age: Int, item: ItemStack): Boolean {
        val bush = Block.SWEET_BERRY_BUSH.withProperty("age", age.toString()).withHandler(handler)
        instance.setBlock(pos, bush)
        val player = mock<Player> {
            on { itemInMainHand } doReturn item
            on { gameMode } doReturn GameMode.CREATIVE
        }
        val interaction =
            BlockHandler.Interaction(bush, instance, BlockFace.TOP, pos, Vec.ZERO, player, PlayerHand.MAIN)
        return handler.onInteract(interaction)
    }

    @Test
    fun matureBushIsHarvested() {
        val pos = Pos(8.0, 64.0, 8.0)
        val handled = interact(pos, 3, ItemStack.AIR)

        Assertions.assertFalse(handled, "interaction should be consumed")
        Assertions.assertEquals("1", instance.getBlock(pos).getProperty("age"))
    }

    @Test
    fun partiallyGrownBushIsHarvested() {
        val pos = Pos(8.0, 64.0, 8.0)
        val handled = interact(pos, 2, ItemStack.AIR)

        Assertions.assertFalse(handled)
        Assertions.assertEquals("1", instance.getBlock(pos).getProperty("age"))
    }

    @Test
    fun boneMealGrowsImmatureBush() {
        val pos = Pos(8.0, 64.0, 8.0)
        interact(pos, 0, ItemStack.of(Material.BONE_MEAL))

        Assertions.assertEquals("1", instance.getBlock(pos).getProperty("age"))
    }

    @Test
    fun immatureBushWithEmptyHandIsUnchanged() {
        val pos = Pos(8.0, 64.0, 8.0)
        interact(pos, 0, ItemStack.AIR)

        Assertions.assertEquals("0", instance.getBlock(pos).getProperty("age"))
    }

    @Test
    fun harvestsThroughMinestomListener() {
        val pos = Pos(8.0, 64.0, 8.0)
        instance.setBlock(pos, Block.SWEET_BERRY_BUSH.withProperty("age", "3").withHandler(handler))

        val player =
            mock<Player> {
                on { getInstance() } doReturn instance
                on { getItemInHand(any()) } doReturn ItemStack.AIR
                on { itemInMainHand } doReturn ItemStack.AIR
                on { getInventory() } doReturn mock<PlayerInventory>()
                on { heldSlot } doReturn 0.toByte()
                on { gameMode } doReturn GameMode.CREATIVE
            }
        val packet =
            ClientPlayerBlockPlacementPacket(PlayerHand.MAIN, pos, BlockFace.TOP, 0.5f, 0.5f, 0.5f, false, false, 0)

        BlockPlacementListener.listener(packet, player)

        Assertions.assertEquals("1", instance.getBlock(pos).getProperty("age"))
    }
}
