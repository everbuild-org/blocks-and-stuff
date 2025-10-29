package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import org.everbuild.blocksandstuff.blocks.util.FallingBlock
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.getNearestHorizontalLookingDirection

class ScaffoldingPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val instance = placementState.instance as Instance
        val placePos = BlockVec(placementState.placePosition)
        val currentBlock = instance.getBlock(placementState.placePosition)
        val face = placementState.blockFace ?: return placementState.block

        if (placementState.isPlayerShifting) {
            val bottomSupport = hasBottomSupport(instance, placePos)
            val nearest = getDistanceToNearestBottomSupported(instance, placePos, bottomSupport)

            if (nearest == null || nearest > 6) {
                return null
            }

            return placementState.block()
                .withProperty("bottom", (!bottomSupport).toString())
                .withProperty("distance", nearest.toString())
        }

        val targetBlockPos = BlockVec(placementState.placePosition.relative(face.oppositeFace))
        val targetBlock = placementState.instance.getBlock(targetBlockPos)

        if (face.toDirection().horizontal()) {
            return verticalPlacement(targetBlock, instance, targetBlockPos, placePos, currentBlock, placementState)
        } else if (face == BlockFace.TOP) {
            return horizontalPlacement(targetBlock, placementState, instance, targetBlockPos, currentBlock, placePos)
        }

        return null
    }

    private fun horizontalPlacement(
        targetBlock: Block,
        placementState: PlacementState,
        instance: Instance,
        targetBlockPos: BlockVec,
        currentBlock: Block?,
        placePos: BlockVec
    ): Block? {
        return if (targetBlock.compare(Block.SCAFFOLDING)) {
            val direction = BlockFace.fromDirection(placementState.getNearestHorizontalLookingDirection().opposite())
            val chainEnd = getChainEnd(instance, targetBlockPos, direction) ?: return null
            val chainEndBlock = instance.getBlock(chainEnd)
            if (chainEndBlock.isAir || chainEndBlock.registry()?.isReplaceable == true) {
                val bottomSupport = hasBottomSupport(instance, chainEnd)
                val nearest = getDistanceToNearestBottomSupported(instance, chainEnd, bottomSupport)

                if (nearest == null || nearest > 6) {
                    FallingBlock.spawn(Block.SCAFFOLDING, instance, chainEnd)
                    return currentBlock
                }

                instance.setBlock(
                    chainEnd,
                    Block.SCAFFOLDING
                        .withProperty("waterlogged", "false")
                        .withProperty("bottom", (!bottomSupport).toString())
                        .withProperty("distance", nearest.toString())
                )
                return currentBlock
            }
            null
        } else {
            if (hasBottomSupport(instance, placePos)) placementState.block
            else null
        }
    }

    private fun verticalPlacement(
        targetBlock: Block,
        instance: Instance,
        targetBlockPos: BlockVec,
        placePos: BlockVec,
        currentBlock: Block?,
        placementState: PlacementState
    ): Block? {
        return if (targetBlock.compare(Block.SCAFFOLDING)) {
            val chainTop = getChainEnd(
                instance,
                targetBlockPos,
                BlockFace.TOP
            ) ?: return null

            val chainTopBlock = instance.getBlock(chainTop)
            if (chainTopBlock.isAir || chainTopBlock.registry()?.isReplaceable == true) {
                instance.setBlock(
                    chainTop,
                    Block.SCAFFOLDING
                        .withProperty("waterlogged", "false")
                        .withProperty("distance", "0")
                )
                return currentBlock
            }
            null
        } else {
            if (hasBottomSupport(instance, placePos)) placementState.block
            else null
        }
    }

    override fun blockUpdate(updateState: UpdateState): Block? {
        val instance = updateState.instance as Instance
        val position = BlockVec(updateState.blockPosition)
        val hasBottomSupport = hasBottomSupport(instance, position)
        val distance = getDistanceToNearestBottomSupported(instance, position, hasBottomSupport)

        if (distance == null || distance > 6) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        instance.setBlock(
            position,
            updateState.currentBlock
                .withProperty("bottom", (!hasBottomSupport).toString())
                .withProperty("distance", distance.toString())
        )

        return updateState.currentBlock
    }

    private fun hasBottomSupport(instance: Block.Getter, position: BlockVec): Boolean {
        val block = instance.getBlock(position.sub(0.0, 1.0, 0.0))
        return block.isSolid
                || block.compare(Block.SCAFFOLDING)
                || block.registry()?.collisionShape()?.isFaceFull(BlockFace.BOTTOM) ?: false
    }

    private fun getChainEnd(instance: Block.Getter, position: BlockVec, direction: BlockFace): BlockVec? {
        repeat(300) { n ->
            val point = position.add(direction.toDirection().vec().mul(n.toDouble() + 1.0))
            val block = instance.getBlock(point)
            if (!block.compare(Block.SCAFFOLDING)) {
                return BlockVec(point)
            }
        }
        return null
    }

    private fun getDistanceToNearestBottomSupported(
        instance: Block.Getter,
        position: BlockVec,
        isBottomSupported: Boolean
    ): Int? {
        if (isBottomSupported) return 0

        val aroundDirections = BlockFace.entries.filter { it.toDirection().horizontal() }
        val minDistance = aroundDirections
            .map { instance.getBlock(position.add(it.toDirection().vec())) }
            .filter { it.compare(Block.SCAFFOLDING) }
            .mapNotNull { it.getProperty("distance")?.toIntOrNull() }
            .minOrNull() ?: return null
        return minDistance + 1
    }

    override fun maxUpdateDistance(): Int = 100
}