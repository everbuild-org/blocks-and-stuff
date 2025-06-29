package org.everbuild.blocksandstuff.blocks.placement.common

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.utils.Direction

abstract class AbstractPlaceAroundCursorRule(block: Block, private val directions: Set<Direction>) :
    BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val prevBlock = placementState.instance.getBlock(placementState.placePosition)
        val assignedDirections = if (prevBlock.compare(block, Block.Comparator.ID))
            directions.filter { prevBlock.getProperty(it.name.lowercase()).toBoolean() }
        else emptySet()

        val possibleDirections = directions
            .filter { isPlacementSupported(placementState.instance, placementState.placePosition, it) }
            .filter { !assignedDirections.contains(it) }

        if (possibleDirections.isEmpty()) return if (assignedDirections.isEmpty()) null else prevBlock
        val direction = placementState.playerPosition?.direction() ?: return null

        val nearest = possibleDirections.maxBy { direction.dot(it.vec()) }

        return (if (prevBlock.compare(block, Block.Comparator.ID)) prevBlock else placementState.block)
            .withProperty(nearest.name.lowercase(), "true")
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val newDirections = directions
            .filter { updateState.currentBlock.getProperty(it.name.lowercase()).toBoolean() }
            .filter { isSupported(updateState.instance, updateState.blockPosition, it) }

        if (newDirections.isEmpty()) {
            return Block.AIR
        }

        return directions.filter { !newDirections.contains(it) }
            .fold(newDirections.fold(updateState.currentBlock) { block, dir ->
                block.withProperty(
                    dir.name.lowercase(),
                    "true"
                )
            }) { block, dir ->
                block.withProperty(
                    dir.name.lowercase(),
                    "false"
                )
            }
    }

    override fun isSelfReplaceable(replacement: Replacement): Boolean {
        return true
    }

    abstract fun isSupported(instance: Block.Getter, position: Point, direction: Direction): Boolean
    abstract fun isPlacementSupported(instance: Block.Getter, position: Point, direction: Direction): Boolean

    protected fun isBackSupported(instance: Block.Getter, position: Point, direction: Direction): Boolean {
        val block = instance.getBlock(position.add(direction.vec()))
        return block.registry().collisionShape().isFaceFull(BlockFace.fromDirection(direction).oppositeFace)
    }
}