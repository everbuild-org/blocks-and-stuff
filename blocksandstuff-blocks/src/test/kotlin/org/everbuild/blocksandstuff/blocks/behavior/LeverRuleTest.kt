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

class LeverRuleTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addLevers() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:lever"))
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun leverTogglesPoweredState() {
        val pos = Pos(8.0, 64.0, 8.0)
        instance.setBlock(pos, Block.LEVER.withHandler(handler))

        interact(pos)
        Assertions.assertEquals("true", instance.getBlock(pos).getProperty("powered"))

        interact(pos)
        Assertions.assertEquals("false", instance.getBlock(pos).getProperty("powered"))
    }

    private fun interact(pos: Pos) {
        val block = instance.getBlock(pos)
        val player = mock<Player> {
            on { isSneaking } doReturn false
            on { itemInMainHand } doReturn ItemStack.AIR
        }
        handler.onInteract(BlockHandler.Interaction(block, instance, BlockFace.TOP, pos, Vec.ZERO, player, PlayerHand.MAIN))
    }
}
