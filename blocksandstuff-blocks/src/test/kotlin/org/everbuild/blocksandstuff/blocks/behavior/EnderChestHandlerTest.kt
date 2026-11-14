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
import net.minestom.server.inventory.Inventory
import org.everbuild.blocksandstuff.blocks.ContainerRegistrations
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.UUID

class EnderChestHandlerTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 65.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        ContainerRegistrations.registerDefault()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun player(id: UUID): Player =
        mock {
            on { uuid } doReturn id
            on { isSneaking } doReturn false
            on { openInventory(any()) } doReturn true
        }

    private fun interact(
        handler: BlockHandler,
        player: Player,
    ) {
        handler.onInteract(
            BlockHandler.Interaction(Block.ENDER_CHEST, instance, BlockFace.TOP, position, Vec.ZERO, player, PlayerHand.MAIN),
        )
    }

    @Test
    fun samePlayerReusesInventory() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:ender_chest"))
        val player = player(UUID.randomUUID())
        val captor = argumentCaptor<Inventory>()

        interact(handler, player)
        interact(handler, player)

        verify(player, times(2)).openInventory(captor.capture())
        Assertions.assertSame(captor.firstValue, captor.secondValue)
    }

    @Test
    fun differentPlayersGetDifferentInventories() {
        val handler = requireNotNull(MinecraftServer.getBlockManager().getHandler("minecraft:ender_chest"))
        val first = player(UUID.randomUUID())
        val second = player(UUID.randomUUID())
        val firstCaptor = argumentCaptor<Inventory>()
        val secondCaptor = argumentCaptor<Inventory>()

        interact(handler, first)
        interact(handler, second)

        verify(first).openInventory(firstCaptor.capture())
        verify(second).openInventory(secondCaptor.capture())
        Assertions.assertNotSame(firstCaptor.firstValue, secondCaptor.firstValue)
    }
}
