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
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class BoneMealRuleTest {
    private lateinit var instance: Instance
    private val position = Pos(4.0, 64.0, 4.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun context(
        block: Block,
        item: Material = Material.BONE_MEAL,
    ): InteractionActivityContext {
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.CREATIVE
                on { itemInMainHand } doReturn ItemStack.of(item)
            }
        return InteractionActivityContext(block, instance, BlockFace.TOP, position.asBlockVec(), Vec.ZERO, player, PlayerHand.MAIN)
    }

    @Test
    fun advancesCropAge() {        instance.setBlock(position, Block.WHEAT.withProperty("age", "0"))

        AGE_BONE_MEAL_RULE.evaluate(context(Block.WHEAT.withProperty("age", "0")))

        val age = instance.getBlock(position).getProperty("age")!!.toInt()
        Assertions.assertTrue(age in 2..7, "expected advanced age, got $age")
    }

    @Test
    fun saplingGrowsIntoTree() {
        instance.setBlock(position, Block.OAK_SAPLING)

        SAPLING_BONE_MEAL_RULE.evaluate(context(Block.OAK_SAPLING))

        Assertions.assertEquals(Block.OAK_LOG.key(), instance.getBlock(position).key())
    }

    @Test
    fun azaleaGrowsIntoTree() {
        instance.setBlock(position, Block.AZALEA)

        SAPLING_BONE_MEAL_RULE.evaluate(context(Block.AZALEA))

        Assertions.assertEquals(Block.OAK_LOG.key(), instance.getBlock(position).key())
    }

    @Test
    fun grassGrowsVegetation() {        instance.setBlock(position, Block.GRASS_BLOCK)

        GRASS_BONE_MEAL_RULE.evaluate(context(Block.GRASS_BLOCK))

        Assertions.assertFalse(instance.getBlock(position.add(0.0, 1.0, 0.0)).air())
    }

    @Test
    fun withoutBoneMealNothingHappens() {
        instance.setBlock(position, Block.WHEAT.withProperty("age", "0"))

        AGE_BONE_MEAL_RULE.evaluate(context(Block.WHEAT.withProperty("age", "0"), item = Material.STICK))

        Assertions.assertEquals("0", instance.getBlock(position).getProperty("age"))
    }
}
