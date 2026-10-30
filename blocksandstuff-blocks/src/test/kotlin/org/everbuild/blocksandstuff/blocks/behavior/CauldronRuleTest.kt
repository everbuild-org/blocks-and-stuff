package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
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
import net.minestom.server.item.component.PotionContents
import net.minestom.server.potion.PotionType
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CauldronRuleTest {
    private lateinit var instance: Instance
    private lateinit var handler: BlockHandler
    private val position = Pos(8.0, 64.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        BlockRuleset.applyRulesAsDefault(BlockRuleset.build { addCauldrons() })
        handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:cauldron"))
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
    ): ItemStack {
        instance.setBlock(position, block.withHandler(handler))
        var result = item
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.SURVIVAL
                on { getItemInHand(any()) } doReturn item
                on { setItemInHand(any(), any()) } doAnswer {
                    result = it.getArgument(1)
                    Unit
                }
            }
        handler.onInteract(
            BlockHandler.Interaction(instance.getBlock(position), instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN),
        )
        return result
    }

    @Test
    fun waterBucketFillsCauldron() {
        val result = interact(Block.CAULDRON, ItemStack.of(Material.WATER_BUCKET))

        Assertions.assertEquals(Block.WATER_CAULDRON.key(), instance.getBlock(position).key())
        Assertions.assertEquals("3", instance.getBlock(position).getProperty("level"))
        Assertions.assertEquals(Material.BUCKET, result.material())
    }

    @Test
    fun bucketEmptiesFullWaterCauldron() {
        val result = interact(Block.WATER_CAULDRON.withProperty("level", "3"), ItemStack.of(Material.BUCKET))

        Assertions.assertEquals(Block.CAULDRON.key(), instance.getBlock(position).key())
        Assertions.assertEquals(Material.WATER_BUCKET, result.material())
    }

    @Test
    fun glassBottleTakesWaterLevel() {
        val result = interact(Block.WATER_CAULDRON.withProperty("level", "2"), ItemStack.of(Material.GLASS_BOTTLE))

        Assertions.assertEquals("1", instance.getBlock(position).getProperty("level"))
        Assertions.assertEquals(PotionType.WATER, result.get(DataComponents.POTION_CONTENTS)?.potion())
    }

    @Test
    fun waterBottleAddsWaterLevel() {
        val bottle =
            ItemStack
                .of(Material.POTION)
                .with(DataComponents.POTION_CONTENTS, PotionContents(PotionType.WATER))
        val result = interact(Block.WATER_CAULDRON.withProperty("level", "2"), bottle)

        Assertions.assertEquals("3", instance.getBlock(position).getProperty("level"))
        Assertions.assertEquals(Material.GLASS_BOTTLE, result.material())
    }
}
