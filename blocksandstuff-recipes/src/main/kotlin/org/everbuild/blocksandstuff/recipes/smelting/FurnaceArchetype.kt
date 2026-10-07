package org.everbuild.blocksandstuff.recipes.smelting

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.InventoryProperty
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.TransactionOption
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventory
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryArchetype
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.recipes.loader.FuelLoader
import org.everbuild.blocksandstuff.recipes.util.transferInto

abstract class FurnaceArchetype(
    override val title: Component,
    override val inventoryType: InventoryType,
) : BlockInventoryArchetype {
    override val size: Int = 3

    override fun createInventory(backend: PhysicalInventory): BlockInventory = InventoryImpl(backend)

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_FUEL = 1
        const val SLOT_OUTPUT = 2
    }

    inner class Backend(
        blockPos: Point,
        instance: Instance,
    ) : SingleBlockInventoryBackend(this@FurnaceArchetype, blockPos, instance)

    inner class InventoryImpl(
        backend: PhysicalInventory,
    ) : BlockInventory(inventoryType, title, backend) {
        init {
            var globalEventNode: EventNode<Event>? = null

            eventNode().addListener(InventoryOpenEvent::class.java) {
                if (globalEventNode == null) {
                    val eventNode = EventNode.all("furnace-inventory-listener")
                    MinecraftServer.getGlobalEventHandler().addChild(eventNode)

                    eventNode.addListener(InventoryCloseEvent::class.java) {
                        if (it.inventory.viewers.count() != 1) return@addListener
                        MinecraftServer.getGlobalEventHandler().removeChild(eventNode)
                    }

                    eventNode.addListener(InstanceTickEvent::class.java) { onTick(it) }
                }
            }

            eventNode().addListener(InventoryPreClickEvent::class.java) {
                // Shift clicks are handled by the shiftClick override below.
                if (it.click is Click.LeftShift || it.click is Click.RightShift) {
                    return@addListener
                }

                val slot = it.slot
                val cursor = it.player.inventory.cursorItem
                if (slot == SLOT_OUTPUT && !cursor.isAir &&
                    (!cursor.isSimilar(it.clickedItem) || cursor.amount() + it.clickedItem.amount() > cursor.maxStackSize())
                ) {
                    it.isCancelled = true
                    return@addListener
                }
                if (slot == SLOT_FUEL && !(cursor.isAir || FuelLoader.isFuel(cursor))) {
                    it.isCancelled = true
                    return@addListener
                }
            }
        }

        override fun shiftClick(player: Player, slot: Int, button: Int): Boolean {
            val playerInventory = player.inventory

            if (slot < size) {
                // Shift-click inside the furnace -> move the item into the player inventory.
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

            // Shift-click from the player inventory -> route fuel to the fuel slot, everything else to the input slot.
            val clickSlot = slot - size
            val clicked = playerInventory.getItemStack(clickSlot)
            if (clicked.isAir) return false

            val target = if (FuelLoader.isFuel(clicked)) SLOT_FUEL else SLOT_INPUT
            val leftover = transferInto(clicked, this, listOf(target))

            playerInventory.setItemStack(clickSlot, leftover)
            playerInventory.update()
            update(player)
            return true
        }

        fun onTick(ignored: InstanceTickEvent) {
            val tags = backend.readTags()
            val litTimeRemaining = tags.getTag(AbstractSmeltingHandler.litTimeRemaining)
            val litTotalTime = tags.getTag(AbstractSmeltingHandler.litTotalTime)
            val cookingTimeTotal = tags.getTag(AbstractSmeltingHandler.cookingTotalTime)
            val cookingTimeSpent = tags.getTag(AbstractSmeltingHandler.cookingTimeSpent)

            this.sendProperty(InventoryProperty.FURNACE_MAXIMUM_FUEL_BURN_TIME, litTotalTime.toShort())
            this.sendProperty(InventoryProperty.FURNACE_FIRE_ICON, litTimeRemaining)
            this.sendProperty(InventoryProperty.FURNACE_PROGRESS_ARROW, cookingTimeSpent.toShort())
            this.sendProperty(InventoryProperty.FURNACE_MAXIMUM_PROGRESS, cookingTimeTotal.toShort())
        }
    }
}
