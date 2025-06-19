package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.InventoryType

object BlockBehaviorRuleRegistrations {
    @JvmStatic
    fun registerDefault() {
        val handler = MinecraftServer.getGlobalEventHandler()
        val manager = MinecraftServer.getBlockManager()
        handler.addListener(PlayerBlockPlaceEvent::class.java) {
            val handler = MinecraftServer.getBlockManager().getHandler(it.block.key().asString())
            if (it.block.handler() != handler) it.block = it.block.withHandler(handler)
        }

        manager.registerHandler(Block.CRAFTING_TABLE.key()) { GenericWorkStationRule(Block.CRAFTING_TABLE, InventoryType.CRAFTING, "Crafting") }
        manager.registerHandler(Block.ANVIL.key()) { GenericWorkStationRule(Block.ANVIL, InventoryType.ANVIL, "Repair & Name") }
        manager.registerHandler(Block.BREWING_STAND.key()) { GenericWorkStationRule(Block.BREWING_STAND, InventoryType.BREWING_STAND, "Brewing Stand") }
        manager.registerHandler(Block.LOOM.key()) { GenericWorkStationRule(Block.LOOM, InventoryType.LOOM, "Loom") }
        manager.registerHandler(Block.GRINDSTONE.key()) { GenericWorkStationRule(Block.GRINDSTONE, InventoryType.GRINDSTONE, "Repair & Disenchant") }
        manager.registerHandler(Block.SMITHING_TABLE.key()) { GenericWorkStationRule(Block.SMITHING_TABLE, InventoryType.SMITHING, "Upgrade Gear") }
        manager.registerHandler(Block.CARTOGRAPHY_TABLE.key()) { GenericWorkStationRule(Block.CARTOGRAPHY_TABLE, InventoryType.CARTOGRAPHY, "Cartography Table") }
        manager.registerHandler(Block.STONECUTTER.key()) { GenericWorkStationRule(Block.STONECUTTER, InventoryType.STONE_CUTTER, "Stonecutter") }
        manager.registerHandler(Block.ENCHANTING_TABLE.key()) { GenericWorkStationRule(Block.ENCHANTING_TABLE, InventoryType.ENCHANTMENT, "Enchant") }

        val copperBlocks = CopperOxidationRule.getOxidizableBlocks()
        for (copperBlock in copperBlocks) {
            val copperOxidationRule = CopperOxidationRule(copperBlock)
            manager.registerHandler(copperBlock.key()) { copperOxidationRule }
        }
    }
}
