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

class HoeRuleTest {
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

    private fun context(block: Block): InteractionActivityContext {
        val player =
            mock<Player> {
                on { gameMode } doReturn GameMode.CREATIVE
                on { itemInMainHand } doReturn ItemStack.of(Material.IRON_HOE)
            }
        return InteractionActivityContext(
            block,
            instance,
            BlockFace.TOP,
            position.asBlockVec(),
            Vec.ZERO,
            player,
            PlayerHand.MAIN,
        )
    }

    @Test
    fun hoeTillsDirtToFarmland() {
        instance.setBlock(position, Block.DIRT)

        HOEING_RULE.evaluate(context(Block.DIRT))

        Assertions.assertEquals(Block.FARMLAND.key(), instance.getBlock(position).key())
    }

    @Test
    fun doesNotTillWhenBlockedAbove() {
        instance.setBlock(position, Block.DIRT)
        instance.setBlock(position.add(0.0, 1.0, 0.0), Block.STONE)

        HOEING_RULE.evaluate(context(Block.DIRT))

        Assertions.assertEquals(Block.DIRT.key(), instance.getBlock(position).key())
    }

    @Test
    fun rootedDirtBecomesDirt() {
        instance.setBlock(position, Block.ROOTED_DIRT)

        HOEING_RULE.evaluate(context(Block.ROOTED_DIRT))

        Assertions.assertEquals(Block.DIRT.key(), instance.getBlock(position).key())
    }
}
