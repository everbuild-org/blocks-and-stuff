package org.everbuild.blocksandstuff.blocks

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.inventory.InventoryType
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.blocks.behavior.BlockContainerHandler
import org.everbuild.blocksandstuff.blocks.behavior.ContainerAnimation
import org.everbuild.blocksandstuff.blocks.behavior.ContainerArchetype
import org.everbuild.blocksandstuff.blocks.behavior.EnderChestHandler

object ContainerRegistrations {
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
        MinecraftServer.getBlockManager().registerHandler("minecraft:ender_chest") { EnderChestHandler() }
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
