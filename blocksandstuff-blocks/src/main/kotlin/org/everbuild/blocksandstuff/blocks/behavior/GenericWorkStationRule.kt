package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryDrops
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler

class GenericWorkStationRule(
    private val block: Block,
    private val type: InventoryType,
    private val title: String,
    private val returnSlots: Collection<Int> = emptyList(),
) : BlockInventoryHandler() {
    override fun getKey(): Key {
        return block.key()
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking && !interaction.player.itemInMainHand.isAir) return true
        val inventory = createInventory()
        trackInventory(inventory, interaction.blockPosition)
        interaction.player.openInventory(inventory)
        return false
    }

    /**
     * Opt-in: only if [returnSlots] is non-empty are the items of those slots
     * returned to the player when the inventory is closed.
     */
    private fun createInventory(): Inventory {
        if (returnSlots.isEmpty()) return Inventory(type, Component.translatable(title))
        return object : Inventory(type, Component.translatable(title)), BlockInventoryDrops {
            override val dropSlots: Collection<Int> = returnSlots
        }
    }
}
