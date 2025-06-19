package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType


class GenericWorkStationRule(private val block: Block, private val type: InventoryType, private val title: String) : BlockHandler {

    override fun getKey(): Key {
        return block.key()
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking && !interaction.player.itemInMainHand.isAir) return super.onInteract(interaction)
        interaction.player.openInventory(Inventory(type, title))
        return false
    }

}