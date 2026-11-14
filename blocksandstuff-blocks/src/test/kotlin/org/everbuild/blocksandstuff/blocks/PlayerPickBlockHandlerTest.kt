package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.player.PlayerPickBlockEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class PlayerPickBlockHandlerTest {
    private lateinit var instance: Instance
    private val position = Pos(8.0, 65.0, 8.0)

    @BeforeEach
    fun setup() {
        MinecraftServer.init()
        PlayerPickBlockHandler.enable()
        instance = MinecraftServer.getInstanceManager().createInstanceContainer()
        instance.loadChunk(0, 0).join()
        instance.setBlock(position, Block.STONE)
    }

    @AfterEach
    fun teardown() {
        MinecraftServer.stopCleanly()
    }

    private fun player(
        inventory: PlayerInventory,
        gameMode: GameMode,
    ): Player =
        mock {
            on { this.inventory } doReturn inventory
            on { instance } doReturn this@PlayerPickBlockHandlerTest.instance
            on { this.gameMode } doReturn gameMode
            on { heldSlot } doReturn 0.toByte()
            on { itemInMainHand } doReturn ItemStack.AIR
        }

    private fun pick(player: Player) {
        EventDispatcher.call(
            PlayerPickBlockEvent(player, instance, Block.STONE, position.asBlockVec(), false),
        )
    }

    @Test
    fun survivalSelectsMatchingHotbarSlot() {
        val inventory = PlayerInventory()
        inventory.setItemStack(3, ItemStack.of(Material.STONE, 4))
        val player = player(inventory, GameMode.SURVIVAL)

        pick(player)

        verify(player).setHeldItemSlot(3.toByte())
    }

    @Test
    fun survivalWithoutMatchDoesNothing() {
        val inventory = PlayerInventory()
        inventory.setItemStack(3, ItemStack.of(Material.DIRT, 4))
        val player = player(inventory, GameMode.SURVIVAL)

        pick(player)

        verify(player, never()).setHeldItemSlot(any())
    }

    @Test
    fun creativeWithoutMatchPutsItemInHand() {
        val inventory = PlayerInventory()
        val player = player(inventory, GameMode.CREATIVE)

        pick(player)

        verify(player).itemInMainHand = ItemStack.of(Material.STONE)
    }
}
