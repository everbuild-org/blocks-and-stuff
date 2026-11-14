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

class ComposterRuleTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler
    private val position = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addComposters() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:composter"))
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun interact(
        block: Block,
        item: ItemStack,
    ): Boolean {
        instance.setBlock(position, block.withHandler(handler))
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.SURVIVAL
                on { getItemInHand(any()) } doReturn item
            }
        return handler.onInteract(
            BlockHandler.Interaction(instance.getBlock(position), instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN),
        )
    }

    @Test
    fun emptyComposterAlwaysAcceptsFirstLayer() {
        val handled = interact(Block.COMPOSTER.withProperty("level", "0"), ItemStack.of(Material.WHEAT_SEEDS))

        Assertions.assertFalse(handled)
        Assertions.assertEquals("1", instance.getBlock(position).getProperty("level"))
    }

    @Test
    fun nonCompostableItemFallsThrough() {
        val handled = interact(Block.COMPOSTER.withProperty("level", "0"), ItemStack.of(Material.STONE))

        Assertions.assertTrue(handled)
        Assertions.assertEquals("0", instance.getBlock(position).getProperty("level"))
    }

    @Test
    fun fullComposterBecomesReadyAfterDelay() {
        interact(Block.COMPOSTER.withProperty("level", "6"), ItemStack.of(Material.CAKE))

        Assertions.assertEquals("7", instance.getBlock(position).getProperty("level"))
        repeat(20) {
            handler.tick(BlockHandler.Tick(instance.getBlock(position), instance, position))
        }
        Assertions.assertEquals("8", instance.getBlock(position).getProperty("level"))
    }

    @Test
    fun readyComposterCanBeEmptied() {
        val handled = interact(Block.COMPOSTER.withProperty("level", "8"), ItemStack.AIR)

        Assertions.assertFalse(handled)
        Assertions.assertEquals("0", instance.getBlock(position).getProperty("level"))
    }
}
