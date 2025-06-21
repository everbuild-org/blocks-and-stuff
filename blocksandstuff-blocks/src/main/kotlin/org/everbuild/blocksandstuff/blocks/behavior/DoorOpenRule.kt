package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler

class DoorOpenRule(private val baseDoorBlock: Block) : BlockHandler {
    override fun getKey(): Key {
        return baseDoorBlock.key()
    }
    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val instance = interaction.instance
        val clickedPosition = interaction.blockPosition
        val clickedBlock = interaction.block

        val half = clickedBlock.getProperty("half")
        val isPowered = clickedBlock.getProperty("powered").toBoolean()

        if (isPowered) {
            return false
        }

        if (interaction.player.isSneaking && !interaction.player.itemInMainHand.isAir) {
            return super.onInteract(interaction)
        }

        val currentOpen = clickedBlock.getProperty("open").toBoolean()
        val newOpen = !currentOpen
        val otherHalfPos = if (half == "lower") {
            clickedPosition.add(0.0, 1.0, 0.0)
        } else { // half == "upper"
            clickedPosition.sub(0.0, 1.0, 0.0)
        }

        val otherHalfBlock = instance.getBlock(otherHalfPos)

        if (!otherHalfBlock.compare(baseDoorBlock) || otherHalfBlock.getProperty("half") == half) {
            return false
        }

        val facing = clickedBlock.getProperty("facing")
        val hinge = clickedBlock.getProperty("hinge")

        val updatedBlockState = baseDoorBlock
            .withProperty("facing", facing)
            .withProperty("hinge", hinge)
            .withProperty("open", newOpen.toString())
            .withProperty("powered", "false")

        instance.setBlock(clickedPosition, updatedBlockState.withProperty("half", half))
        instance.setBlock(otherHalfPos, updatedBlockState.withProperty("half", if (half == "lower") "upper" else "lower"))

        return false
    }
}