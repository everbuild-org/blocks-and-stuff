package org.everbuild.blocksandstuff.blocks

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.inventory.InventoryType
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.BlockContainerHandler
import org.everbuild.blocksandstuff.blocks.behavior.CrafterHandler
import org.everbuild.blocksandstuff.blocks.behavior.CrafterSessions
import org.everbuild.blocksandstuff.blocks.behavior.ContainerAnimation
import org.everbuild.blocksandstuff.blocks.behavior.ContainerArchetype
import org.everbuild.blocksandstuff.blocks.behavior.DispenserHandler
import org.everbuild.blocksandstuff.blocks.behavior.DropperHandler
import org.everbuild.blocksandstuff.blocks.behavior.EnderChestHandler
import org.everbuild.blocksandstuff.blocks.behavior.HopperHandler

object ContainerRegistrations {
    private const val EJECTOR_SIZE = 9
    private val colors =
        listOf(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black",
        )

    @JvmStatic
    fun registerDefault() {
        registerChest("minecraft:chest")
        registerChest("minecraft:trapped_chest")
        register(
            "minecraft:barrel",
            "container.barrel",
            InventoryType.CHEST_3_ROW,
            SoundEvent.BLOCK_BARREL_OPEN,
            SoundEvent.BLOCK_BARREL_CLOSE,
            ContainerAnimation.OPEN_PROPERTY,
        )
        registerShulker("minecraft:shulker_box")
        for (color in colors) {
            registerShulker("minecraft:${color}_shulker_box")
        }
        registerHopper()
        registerDropper()
        registerDispenser()
        MinecraftServer.getBlockManager().registerHandler("minecraft:crafter") { CrafterHandler() }
        CrafterSessions.registerDefault()
        MinecraftServer.getBlockManager().registerHandler("minecraft:ender_chest") { EnderChestHandler() }
    }

    private fun registerHopper() {
        val key = Key.key("minecraft:hopper")
        val archetype =
            ContainerArchetype(
                Component.translatable("container.hopper"),
                InventoryType.HOPPER,
                InventoryType.HOPPER.size,
            )
        MinecraftServer.getBlockManager().registerHandler("minecraft:hopper") {
            HopperHandler(key, archetype)
        }
    }

    private fun registerDropper() {
        val key = Key.key("minecraft:dropper")
        val archetype =
            ContainerArchetype(
                Component.translatable("container.dropper"),
                InventoryType.WINDOW_3X3,
                EJECTOR_SIZE,
            )
        MinecraftServer.getBlockManager().registerHandler("minecraft:dropper") {
            DropperHandler(key, archetype)
        }
    }

    private fun registerDispenser() {
        val key = Key.key("minecraft:dispenser")
        val archetype =
            ContainerArchetype(
                Component.translatable("container.dispenser"),
                InventoryType.WINDOW_3X3,
                EJECTOR_SIZE,
            )
        MinecraftServer.getBlockManager().registerHandler("minecraft:dispenser") {
            DispenserHandler(key, archetype)
        }
    }
    private fun registerChest(blockKey: String) {
        register(
            blockKey,
            "container.chest",
            InventoryType.CHEST_3_ROW,
            SoundEvent.BLOCK_CHEST_OPEN,
            SoundEvent.BLOCK_CHEST_CLOSE,
            ContainerAnimation.BLOCK_EVENT,
        )
    }

    private fun registerShulker(blockKey: String) {
        register(
            blockKey,
            "container.shulkerBox",
            InventoryType.SHULKER_BOX,
            SoundEvent.BLOCK_SHULKER_BOX_OPEN,
            SoundEvent.BLOCK_SHULKER_BOX_CLOSE,
            ContainerAnimation.BLOCK_EVENT,
            keepContentsInItem = true,
        )
    }

    private fun register(
        blockKey: String,
        titleKey: String,
        inventoryType: InventoryType,
        openSound: SoundEvent,
        closeSound: SoundEvent,
        animation: ContainerAnimation,
        keepContentsInItem: Boolean = false,
    ) {
        val key = Key.key(blockKey)
        val archetype = ContainerArchetype(Component.translatable(titleKey), inventoryType, inventoryType.size)
        MinecraftServer.getBlockManager().registerHandler(blockKey) {
            BlockContainerHandler(key, archetype, openSound, closeSound, animation, keepContentsInItem)
        }
    }
}
