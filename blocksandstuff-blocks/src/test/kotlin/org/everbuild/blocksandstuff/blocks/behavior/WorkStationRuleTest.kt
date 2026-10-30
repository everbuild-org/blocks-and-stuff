package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class WorkStationRuleTest {
    private lateinit var instance: Instance

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addWorkStations() })
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun interactOpensInventory() {
        val block = Block.CRAFTING_TABLE
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler(block.key().asString()))
        val pos = Pos(8.0, 64.0, 8.0)
        val placed = block.withHandler(handler)
        instance.setBlock(pos, placed)

        val player = mock<Player> {
            on { isSneaking } doReturn false
            on { itemInMainHand } doReturn ItemStack.AIR
            on { openInventory(any()) } doReturn true
        }

        val handled =
            handler.onInteract(
                net.minestom.server.instance.block.BlockHandler.Interaction(
                    placed,
                    instance,
                    BlockFace.TOP,
                    pos,
                    Vec.ZERO,
                    player,
                    PlayerHand.MAIN,
                ),
            )

        Assertions.assertFalse(handled)
        verify(player).openInventory(any<Inventory>())
    }
}
