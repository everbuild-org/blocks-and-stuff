package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.behavior.canFireSurvive
import org.everbuild.blocksandstuff.blocks.behavior.isFlammable

class FirePlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block {
        return withFireShape(
            placementState.instance as Instance,
            placementState.placePosition,
            placementState.block,
        )
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val instance = updateState.instance as Instance
        if (!canFireSurvive(instance, updateState.blockPosition)) return Block.AIR

        return withFireShape(instance, updateState.blockPosition, updateState.currentBlock)
    }

    override fun maxUpdateDistance(): Int = 3

    private fun withFireShape(
        instance: Instance,
        position: net.minestom.server.coordinate.Point,
        block: Block,
    ): Block {
        if (block.compare(Block.SOUL_FIRE)) return block

        var fire = block
        for (face in HORIZONTAL_FACES) {
            fire = fire.withProperty(
                face.name.lowercase(),
                isFlammable(instance.getBlock(position.relative(face))).toString(),
            )
        }
        fire = fire.withProperty(
            "up",
            isFlammable(instance.getBlock(position.add(0.0, 1.0, 0.0))).toString(),
        )
        return fire
    }

    private companion object {
        private val HORIZONTAL_FACES = listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
    }
}
