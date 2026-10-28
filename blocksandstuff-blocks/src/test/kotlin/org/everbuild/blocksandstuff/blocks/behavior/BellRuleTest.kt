package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class BellRuleTest {
    private lateinit var instance: Instance
    private val position = Pos(4.0, 64.0, 4.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        BellInteraction.enable()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun interact(block: Block): PlayerBlockInteractEvent {
        val player =
            mock<Player> {
                on { isSneaking } doReturn false
                on { itemInMainHand } doReturn ItemStack.AIR
            }
        val event =
            PlayerBlockInteractEvent(
                player,
                PlayerHand.MAIN,
                instance,
                block,
                position.asBlockVec(),
                Vec.ZERO,
                BlockFace.TOP,
            )
        EventDispatcher.call(event)
        return event
    }

    @Test
    fun rightClickRingsBell() {
        val event = interact(Block.BELL)

        Assertions.assertTrue(event.isBlockingItemUse)
    }

    @Test
    fun otherBlocksAreNotConsumed() {
        val event = interact(Block.STONE)

        Assertions.assertFalse(event.isBlockingItemUse)
    }
}
