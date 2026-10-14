package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
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

class CandleCakeRuleTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addCandleCakes() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:candle_cake"))
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun eatingCandleCakeLeavesCake() {
        val pos = Pos(8.0, 64.0, 8.0)
        val cake = Block.CANDLE_CAKE.withHandler(handler)
        instance.setBlock(pos, cake)
        val player = mock<Player> {
            on { isSneaking } doReturn false
            on { itemInMainHand } doReturn ItemStack.AIR
            on { getItemInHand(any()) } doReturn ItemStack.AIR
        }

        val handled =
            handler.onInteract(
                BlockHandler.Interaction(cake, instance, BlockFace.TOP, pos, Vec.ZERO, player, PlayerHand.MAIN),
            )

        Assertions.assertFalse(handled)
        val result = instance.getBlock(pos)
        Assertions.assertTrue(result.compare(Block.CAKE), "expected cake, got ${result.name()}")
        Assertions.assertEquals("1", result.getProperty("bites"))
    }

    @Test
    fun flintAndSteelLightsCandleCake() {
        val pos = Pos(8.0, 64.0, 8.0)
        val cake = Block.CANDLE_CAKE.withHandler(handler)
        instance.setBlock(pos, cake)
        val flint = ItemStack.of(Material.FLINT_AND_STEEL)
        val player = mock<Player> {
            on { isSneaking } doReturn false
            on { itemInMainHand } doReturn flint
            on { getItemInHand(any()) } doReturn flint
        }

        val handled =
            handler.onInteract(
                BlockHandler.Interaction(cake, instance, BlockFace.TOP, pos, Vec.ZERO, player, PlayerHand.MAIN),
            )

        Assertions.assertFalse(handled)
        val result = instance.getBlock(pos)
        Assertions.assertTrue(result.compare(Block.CANDLE_CAKE), "expected candle cake, got ${result.name()}")
        Assertions.assertEquals("true", result.getProperty("lit"))
    }
}
