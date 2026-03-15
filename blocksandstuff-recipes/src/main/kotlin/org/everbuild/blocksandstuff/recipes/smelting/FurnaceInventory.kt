package org.everbuild.blocksandstuff.recipes.smelting

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.instance.Instance
import net.minestom.server.inventory.InventoryProperty
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.recipes.api.BlockInventory

class FurnaceInventory(
    private val blockPos: Point,
    private val instance: Instance,
    title: Component = Component.translatable("container.furnace"),
    inventoryType: InventoryType = InventoryType.FURNACE
) : BlockInventory(inventoryType, title, FurnaceInventoryBackend(blockPos, instance)) {

    fun onInventoryOpen() {
        val eventNode = EventNode.all("furnace-inventory-listener")
        MinecraftServer.getGlobalEventHandler().addChild(eventNode)

        eventNode.addListener(InventoryCloseEvent::class.java) {
            MinecraftServer.getGlobalEventHandler().removeChild(eventNode)
        }

        eventNode.addListener(InstanceTickEvent::class.java) { onTick(it) }

    }

    fun onTick(ignored: InstanceTickEvent) {
        backend.load()

        val furnaceBlock = instance.getBlock(blockPos)
        val litTimeRemaining = furnaceBlock.getTag(AbstractSmeltingHandler.litTimeRemaining)
        val litTotalTime = furnaceBlock.getTag(AbstractSmeltingHandler.litTotalTime)
        val cookingTimeTotal = furnaceBlock.getTag(AbstractSmeltingHandler.cookingTotalTime)
        val cookingTimeSpent = furnaceBlock.getTag(AbstractSmeltingHandler.cookingTimeSpent)

        this.sendProperty(InventoryProperty.FURNACE_MAXIMUM_FUEL_BURN_TIME, litTotalTime.toShort())
        this.sendProperty(InventoryProperty.FURNACE_FIRE_ICON, litTimeRemaining.toShort())
        this.sendProperty(InventoryProperty.FURNACE_PROGRESS_ARROW, cookingTimeSpent.toShort())
        this.sendProperty(InventoryProperty.FURNACE_MAXIMUM_PROGRESS, cookingTimeTotal.toShort())
    }

    companion object {
        fun withInventoryReload() {
            MinecraftServer.getGlobalEventHandler().addListener(InventoryOpenEvent::class.java) {
                if (it.inventory !is FurnaceInventory) return@addListener
                (it.inventory as FurnaceInventory).onInventoryOpen()
            }
        }
    }
}