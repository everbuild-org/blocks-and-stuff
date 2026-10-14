package org.everbuild.blocksandstuff.blocks.behavior

// <@AI_UNREVIEWED>
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
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CandleLightListenerTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addCandles() })
        PlacedHandlerRegistration.registerDefault()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun playerPlacedCandleCanBeLit() {
        val stonePos = Pos(8.0, 63.0, 8.0)
        val candlePos = Pos(8.0, 64.0, 8.0)
        instance.setBlock(stonePos, Block.STONE)

        val candle = ItemStack.of(Material.CANDLE)
        val player = player(candle)
        BlockPlacementListener.listener(
            packet(stonePos, BlockFace.TOP),
            player,
        )

        val placed = instance.getBlock(candlePos)
        println("PLACED=${placed.name()} handler=${placed.handler()}")

        val flint = ItemStack.of(Material.FLINT_AND_STEEL)
        val flintPlayer = player(flint)
        BlockPlacementListener.listener(
            packet(candlePos, BlockFace.TOP),
            flintPlayer,
        )

        val result = instance.getBlock(candlePos)
        println("RESULT=${result.name()} lit=${result.getProperty("lit")}")
        Assertions.assertEquals("true", result.getProperty("lit"))
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
