package org.everbuild.blocksandstuff.recipes.smelting

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.InventoryProperty
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.Click
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventory
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryArchetype
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend
import org.everbuild.blocksandstuff.recipes.loader.FuelLoader

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
                val slot = it.slot
                if (slot == SLOT_OUTPUT && !it.player.inventory.cursorItem.isAir) {
                    it.isCancelled = true
                    return@addListener
                }
                if (slot == SLOT_FUEL && !(it.player.inventory.cursorItem.isAir || FuelLoader.isFuel(it.player.inventory.cursorItem))) {
                    it.isCancelled = true
                    return@addListener
                }

                if (it.click is Click.LeftShift || it.click is Click.RightShift) {
                    it.isCancelled = true
                    // Todo: implement Shift Clicking when the API in Minestom is implemented
                    return@addListener
                }
                // if (FuelLoader.isFuel(it.clickedItem) && (it.click is Click.LeftShift || it.click is Click.RightShift)) {
                //    if (slot == SLOT_OUTPUT) {
                //        it.isCancelled = true
                //        return@addListener
                //    }
                // }
            }
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
