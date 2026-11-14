package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
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

class TntIgniteRuleTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addTntIgnition() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:tnt"))
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    @Test
    fun flintAndSteelIgnitesTnt() {
        val pos = Pos(8.0, 64.0, 8.0)
        val tnt = Block.TNT.withHandler(handler)
        instance.setBlock(pos, tnt)

        interact(tnt, pos, ItemStack.of(Material.FLINT_AND_STEEL))

        Assertions.assertEquals(Block.AIR.key(), instance.getBlock(pos).key())
        Assertions.assertTrue(instance.entities.any { it.entityType == EntityType.TNT })
    }

    @Test
    fun otherItemDoesNotIgniteTnt() {
        val pos = Pos(8.0, 64.0, 8.0)
        val tnt = Block.TNT.withHandler(handler)
        instance.setBlock(pos, tnt)

        interact(tnt, pos, ItemStack.of(Material.STONE))

        Assertions.assertEquals(Block.TNT.key(), instance.getBlock(pos).key())
        Assertions.assertTrue(instance.entities.none { it.entityType == EntityType.TNT })
    }

    private fun interact(
        tnt: Block,
        pos: Pos,
        item: ItemStack,
    ) {
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.CREATIVE
                on { itemInMainHand } doReturn item
                on { getItemInHand(any()) } doReturn item
            }
        handler.onInteract(
            BlockHandler.Interaction(tnt, instance, BlockFace.TOP, pos, Vec.ZERO, player, PlayerHand.MAIN),
        )
    }
}
