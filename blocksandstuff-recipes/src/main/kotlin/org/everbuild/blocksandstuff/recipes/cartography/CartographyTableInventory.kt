package org.everbuild.blocksandstuff.recipes.cartography

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryDrops
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.recipes.util.excludeResultSlotsFromDrag
import org.everbuild.blocksandstuff.recipes.util.transferInto

class CartographyTableInventory :
    Inventory(InventoryType.CARTOGRAPHY, Component.translatable("container.cartography_table")),
    BlockInventoryDrops {
    companion object {
        const val MAP_SLOT = 0
        const val ADDITIONAL_SLOT = 1
        const val RESULT_SLOT = 2
    }

    override val dropSlots: Collection<Int> = listOf(MAP_SLOT, ADDITIONAL_SLOT)

    init {
        eventNode().addListener(InventoryPreClickEvent::class.java) { event ->
            event.excludeResultSlotsFromDrag(RESULT_SLOT)

            val slot = event.slot
            if (slot !in 0 until size) return@addListener

            // The result slot is never edited manually.
            if (slot == RESULT_SLOT) {
                event.isCancelled = true
                return@addListener
            }

            val cursor = event.player.inventory.cursorItem
            if (cursor.isAir) return@addListener

            if (slotFor(cursor.material()) != slot) {
                event.isCancelled = true
            }
        }
    }

    override fun shiftClick(player: Player, slot: Int, button: Int): Boolean {
        val playerInventory = player.inventory

        if (slot == RESULT_SLOT) return false

        if (slot < size) {
            // Shift-click inside the cartography table -> move the item into the player inventory.
            val clicked = getItemStack(slot)
            if (clicked.isAir) return false

            setItemStack(slot, ItemStack.AIR)
            val leftover = playerInventory.addItemStack(clicked, TransactionOption.ALL)
            if (!leftover.isAir) {
                DroppedItemFactory.maybeDropFromPlayer(player, leftover)
            }

            playerInventory.update()
            update(player)
            return true
        }

        // Shift-click from the player inventory -> route into the matching cartography slot.
        val clickSlot = slot - size
        val clicked = playerInventory.getItemStack(clickSlot)
        if (clicked.isAir) return false

        val target = slotFor(clicked.material()) ?: return false
        val leftover = transferInto(clicked, this, listOf(target))

        playerInventory.setItemStack(clickSlot, leftover)
        playerInventory.update()
        update(player)
        return true
    }

    private fun slotFor(material: Material): Int? = when {
        material == Material.FILLED_MAP -> MAP_SLOT
        material == Material.PAPER -> ADDITIONAL_SLOT
        material == Material.MAP -> ADDITIONAL_SLOT
        material.name().endsWith("glass_pane") -> ADDITIONAL_SLOT
        else -> null
    }
}
