package org.everbuild.blocksandstuff.blocks.placement

import org.everbuild.blocksandstuff.blocks.placement.util.States
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import java.util.*
import java.util.Map

class TrapdoorPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val placementFace = placementState.blockFace()
        val playerPos = placementState.playerPosition() ?: Pos.ZERO
        val direction = BlockFace.fromYaw(playerPos.yaw()).toDirection().opposite()
        val cursorPos = placementState.cursorPosition() ?: Vec.ZERO
        val facing = BlockFace.fromDirection(direction)

        val half = if (placementFace == BlockFace.BOTTOM
            ||
            placementFace != BlockFace.TOP
            &&
            cursorPos.y() > 0.5
        )
            BlockFace.TOP
        else
            BlockFace.BOTTOM

        val block = placementState.block().withProperties(
            Map.of<String, String>(
                States.HALF, half.name.lowercase(Locale.getDefault()),
                States.FACING, facing.name.lowercase(Locale.getDefault())
            )
        )

        return block
    }
}