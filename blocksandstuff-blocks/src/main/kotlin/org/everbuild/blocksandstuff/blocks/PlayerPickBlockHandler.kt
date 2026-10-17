package org.everbuild.blocksandstuff.blocks

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerPickBlockEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.blocks.util.toContainerItem

object PlayerPickBlockHandler {
    fun enable() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerPickBlockEvent::class.java) { event ->
            val player = event.player
            val block = player.instance.getBlock(event.blockPosition)
            val item = block.intoItem(event.isIncludeData) ?: return@addListener

            when (player.gameMode) {
                GameMode.CREATIVE -> pickCreative(player, item)
                GameMode.SURVIVAL, GameMode.ADVENTURE -> pickSurvival(player, item)
                else -> Unit
            }
        }
    }

    private fun pickCreative(
        player: Player,
        item: ItemStack,
    ) {
        val hotbarSlot = matchingItemInHotbar(player, item)
        if (hotbarSlot != null) {
            player.setHeldItemSlot(hotbarSlot.toByte())
            return
        }

        val inventorySlot = matchingItemInInventory(player, item)
        if (inventorySlot == null) {
            val old = player.itemInMainHand
            player.itemInMainHand = item
            player.inventory.addItemStack(old, TransactionOption.ALL)
            return
        }

        swapIntoHeldSlot(player, inventorySlot, item)
    }

    private fun pickSurvival(
        player: Player,
        item: ItemStack,
    ) {
        val hotbarSlot = matchingItemInHotbar(player, item)
        if (hotbarSlot != null) {
            player.setHeldItemSlot(hotbarSlot.toByte())
            return
        }

        val inventorySlot = matchingItemInInventory(player, item) ?: return
        swapIntoHeldSlot(player, inventorySlot, item)
    }

    private fun swapIntoHeldSlot(
        player: Player,
        inventorySlot: Int,
        item: ItemStack,
    ) {
        val hotbarItem = player.itemInMainHand
        player.inventory.setItemStack(player.heldSlot.toInt(), item)
        player.inventory.setItemStack(inventorySlot, hotbarItem)
    }

    private fun matchingItemInHotbar(
        player: Player,
        item: ItemStack,
    ): Int? {
        for (slot in 0 until HOTBAR_SIZE) {
            if (player.inventory.getItemStack(slot).isSimilar(item)) return slot
        }
        return null
    }

    private fun matchingItemInInventory(
        player: Player,
        item: ItemStack,
    ): Int? {
        for (slot in player.inventory.itemStacks.indices) {
            if (player.inventory.getItemStack(slot).isSimilar(item)) return slot
        }
        return null
    }

    private const val HOTBAR_SIZE = 9
}

private fun Block.intoItem(includeData: Boolean): ItemStack? {
    val material = material() ?: return null
    if (!includeData) return ItemStack.of(material)
    return toContainerItem()
}
