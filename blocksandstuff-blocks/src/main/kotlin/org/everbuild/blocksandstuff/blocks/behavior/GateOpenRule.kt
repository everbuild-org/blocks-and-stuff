package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.common.utils.getNearestLookingDirection

class GateOpenRule(private val block: Block?) : BlockHandler {
    override fun getKey(): Key {
        return block?.key() ?: key.key()
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking && !interaction.player.itemInMainHand.isAir)
            return true

        val allowedDirections = getAllowedDirections(interaction.block)
        val direction = interaction.getNearestLookingDirection(allowedDirections).opposite()
        val bool = (!interaction.block.getProperty("open").toBoolean()).toString()
        interaction.instance.setBlock(interaction.blockPosition,
            interaction.block
                .withProperty("open", bool)
                .withProperty("facing", direction.toString().lowercase())
        )
        return false
    }
    
    fun getAllowedDirections(block: Block): Collection<Direction> {
        val currentDirection = Direction.valueOf(block.getProperty("facing").uppercase())
        return listOf(currentDirection, currentDirection.opposite())
    }
}