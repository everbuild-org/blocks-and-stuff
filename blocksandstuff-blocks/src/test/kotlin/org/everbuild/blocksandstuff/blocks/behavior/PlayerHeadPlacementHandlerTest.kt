package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.listener.BlockPlacementListener
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket
import org.everbuild.blocksandstuff.blocks.PlacedHandlerRegistration
import org.everbuild.blocksandstuff.blocks.placement.PlayerHeadPlacementRule
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PlayerHeadPlacementHandlerTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addSkulls() })
        MinecraftServer.getBlockManager().registerBlockPlacementRule(PlayerHeadPlacementRule(Block.PLAYER_HEAD))
        PlacedHandlerRegistration.registerDefault()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun placedPlayerHeadKeepsHandler() {
        val stonePos = Pos(8.0, 63.0, 8.0)
        val headPos = Pos(8.0, 64.0, 8.0)
        instance.setBlock(stonePos, Block.STONE)

        BlockPlacementListener.listener(
            packet(stonePos, BlockFace.TOP),
            player(ItemStack.of(Material.PLAYER_HEAD)),
        )

        val placed = instance.getBlock(headPos)
        Assertions.assertTrue(placed.compare(Block.PLAYER_HEAD), "expected player head, got ${placed.name()}")
        Assertions.assertNotNull(placed.handler(), "placement rule must not strip the block handler")
    }

    private fun packet(
        pos: Pos,
        face: BlockFace,
    ) = ClientPlayerBlockPlacementPacket(PlayerHand.MAIN, pos, face, 0.5f, 0.5f, 0.5f, false, false, 0)

    private fun player(item: ItemStack): Player =
        mock {
            on { getInstance() } doReturn instance
            on { getItemInHand(any()) } doReturn item
            on { itemInMainHand } doReturn item
            on { getInventory() } doReturn mock<PlayerInventory>()
            on { heldSlot } doReturn 0.toByte()
        }
}
