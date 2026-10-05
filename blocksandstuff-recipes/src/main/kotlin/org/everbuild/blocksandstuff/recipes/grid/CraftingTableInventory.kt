package org.everbuild.blocksandstuff.recipes.grid

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryDrops

class CraftingTableInventory : Inventory(InventoryType.CRAFTING, Component.translatable("container.crafting")), BlockInventoryDrops {
    private val service = CraftingTableGridService(this)

    override val dropSlots: Collection<Int> = (1..9).toList()

    init {
        eventNode()
            .addListener(InventoryItemChangeEvent::class.java) { service.onChangeItem(it) }
            .addListener(InventoryPreClickEvent::class.java) { service.onClickItem(it) }
    }

    override fun shiftClick(player: Player, slot: Int, button: Int): Boolean {
        if (slot < size) {
            return super.shiftClick(player, slot, button)
        }

        val playerInventory = player.inventory
        val clickSlot = slot - size
        val clicked = playerInventory.getItemStack(clickSlot)
        val clickResult = clickProcessor.shiftClick(
            playerInventory,
            this,
            1,
            innerSize,
            1,
            player,
            clickSlot,
            clicked,
            playerInventory.cursorItem
        )

        if (clickResult.isCancel) {
            playerInventory.update()
            update(player)
            return false
        }

        playerInventory.setItemStack(clickSlot, clickResult.clicked)
        playerInventory.update()
        update(player)
        playerInventory.cursorItem = clickResult.cursor
        return true
    }
}