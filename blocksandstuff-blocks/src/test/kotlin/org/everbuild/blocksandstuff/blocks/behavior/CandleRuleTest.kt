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
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CandleRuleTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addCandles() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:candle"))
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun flintAndSteelLightsCandle() {
        val pos = Pos(8.0, 64.0, 8.0)
        val candle = Block.CANDLE.withHandler(handler)
        instance.setBlock(pos, candle)

        val handled = interact(candle, pos, ItemStack.of(Material.FLINT_AND_STEEL))

        Assertions.assertFalse(handled)
        Assertions.assertEquals("true", instance.getBlock(pos).getProperty("lit"))
    }

    @Test
    fun emptyHandExtinguishesCandle() {
        val pos = Pos(8.0, 64.0, 8.0)
        val candle = Block.CANDLE.withProperty("lit", "true").withHandler(handler)
        instance.setBlock(pos, candle)

        val handled = interact(candle, pos, ItemStack.AIR)

        Assertions.assertFalse(handled)
        Assertions.assertEquals("false", instance.getBlock(pos).getProperty("lit"))
    }

    @Test
    fun otherItemFallsThrough() {
        val pos = Pos(8.0, 64.0, 8.0)
        val candle = Block.CANDLE.withHandler(handler)
        instance.setBlock(pos, candle)

        val handled = interact(candle, pos, ItemStack.of(Material.STONE))

        Assertions.assertTrue(handled)
        Assertions.assertEquals("false", instance.getBlock(pos).getProperty("lit"))
    }

    @Test
    fun doesNotExtinguishWhenOtherHandHoldsLighter() {
        val pos = Pos(8.0, 64.0, 8.0)
        val candle = Block.CANDLE.withProperty("lit", "true").withHandler(handler)
        instance.setBlock(pos, candle)

        val flint = ItemStack.of(Material.FLINT_AND_STEEL)
        val player = mock<Player> {
            on { gameMode } doReturn GameMode.CREATIVE
            on { getItemInHand(PlayerHand.MAIN) } doReturn ItemStack.AIR
            on { getItemInHand(PlayerHand.OFF) } doReturn flint
        }

        val handled =
            handler.onInteract(
                BlockHandler.Interaction(candle, instance, BlockFace.TOP, pos, Vec.ZERO, player, PlayerHand.MAIN),
            )

        Assertions.assertTrue(handled)
        Assertions.assertEquals("true", instance.getBlock(pos).getProperty("lit"))
    }

    private fun interact(
        candle: Block,
        pos: Pos,
        item: ItemStack,
    ): Boolean {
        val player = mock<Player> {
            on { gameMode } doReturn GameMode.CREATIVE
            on { itemInMainHand } doReturn item
            on { getItemInHand(any()) } doReturn item
        }
        return handler.onInteract(
            BlockHandler.Interaction(candle, instance, BlockFace.TOP, pos, Vec.ZERO, player, PlayerHand.MAIN),
        )
    }
}
