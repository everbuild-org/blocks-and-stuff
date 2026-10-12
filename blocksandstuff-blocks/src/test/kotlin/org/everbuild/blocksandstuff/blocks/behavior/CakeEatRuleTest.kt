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
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CakeEatRuleTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addCakes() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:cake"))
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun eat(pos: Pos, bites: Int): Boolean {
        val cake = Block.CAKE.withProperty("bites", bites.toString()).withHandler(handler)
        instance.setBlock(pos, cake)
        val player = mock<Player> {
            on { isSneaking } doReturn false
            on { itemInMainHand } doReturn ItemStack.AIR
        }
        return handler.onInteract(
            BlockHandler.Interaction(cake, instance, BlockFace.TOP, pos, Vec.ZERO, player, PlayerHand.MAIN),
        )
    }

    @Test
    fun eatingIncrementsBites() {
        val pos = Pos(8.0, 64.0, 8.0)
        Assertions.assertFalse(eat(pos, 0))

        Assertions.assertEquals("1", instance.getBlock(pos).getProperty("bites"))
    }

    @Test
    fun lastBiteRemovesCake() {
        val pos = Pos(8.0, 64.0, 8.0)
        eat(pos, 6)

        Assertions.assertTrue(instance.getBlock(pos).air())
    }
}
