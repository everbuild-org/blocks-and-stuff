package org.everbuild.blocksandstuff.blocks.placement

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.RegistryTag
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.tag.BlockTags

class RedstoneWirePlacementRule(block: Block) : BlockPlacementRule(block) {
    private val simpleConnectionPoints = RegistryTag.direct<Block>(
        listOf(
            Block.REDSTONE_BLOCK,
            Block.CALIBRATED_SCULK_SENSOR,
            Block.SCULK_SENSOR,
            Block.DAYLIGHT_DETECTOR,
            Block.DETECTOR_RAIL,
            Block.JUKEBOX,
            Block.LEVER,
            Block.REDSTONE_TORCH,
            Block.TARGET,
            Block.TRAPPED_CHEST
        )
                + BlockTags.getTaggedWith("minecraft:buttons")
                + BlockTags.getTaggedWith("minecraft:pressure_plates")
    )

    private val horizontalFaces = BlockFace.entries.filter { it.toDirection().horizontal() }

    override fun blockPlace(placementState: PlacementState): Block? {
        return getUpdatedBlock(
            placementState.block,
            placementState.instance,
            placementState.placePosition?.asBlockVec() ?: return placementState.block
        )?.block
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val newBlock = getUpdatedBlock(
            updateState.currentBlock, updateState.instance, updateState.blockPosition.asBlockVec()
        )

        if (newBlock == null) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return newBlock.block
    }

    private fun getUpdatedBlock(current: Block, instance: Block.Getter, position: BlockVec): UpdateResult? {
        val bottomBlock = instance.getBlock(position.sub(0, 1, 0))
        val supported = bottomBlock.registry()?.collisionShape()?.isFaceFull(BlockFace.TOP) ?: false
        if (!supported) return null

        val sideConnections = getSideConnections(instance, position, true)
        val bottomSideConnections = getSideConnections(instance, position.sub(0, 1, 0), false)
        val topSideConnections = getSideConnections(instance, position.add(0, 1, 0), false)

        val connections = horizontalFaces
            .associate { face ->
                when (face) {
                    in topSideConnections -> face to ConnectionType.UP
                    in bottomSideConnections -> face to ConnectionType.DOWN
                    in sideConnections -> face to ConnectionType.DIRECT
                    else -> face to ConnectionType.NONE
                }
            }

        val preliminaryResult = connections.entries.fold(current) { acc, (face, connection) ->
            return@fold acc.withProperty(
                face.name.lowercase(),
                when (connection) {
                    ConnectionType.NONE -> "none"
                    ConnectionType.DIRECT -> "side"
                    ConnectionType.UP -> "up"
                    ConnectionType.DOWN -> "side"
                }
            )
        }

        val actualConnections = connections.filter { it.value != ConnectionType.NONE }
        if (actualConnections.isEmpty()) {
            return UpdateResult(block.withProperties(horizontalFaces.associate { it.name.lowercase() to "side" }), connections)
        }

        if (actualConnections.size == 1) {
            val (face, _) = actualConnections.entries.first()
            return UpdateResult(preliminaryResult.withProperty(face.oppositeFace.name.lowercase(), "side"), connections)
        }

        return UpdateResult(preliminaryResult, connections)
    }

    enum class ConnectionType {
        NONE, DIRECT, UP, DOWN
    }

    data class UpdateResult(val block: Block, val connections: Map<BlockFace, ConnectionType>)

    // Currently, redstone wire only connects to simple blocks
    private fun getSideConnections(
        instance: Block.Getter,
        position: BlockVec,
        direct: Boolean
    ): List<BlockFace> = horizontalFaces
        .filter {
            val block = instance.getBlock(position.relative(it))
            return@filter (if (direct) simpleConnectionPoints.contains(block) else false) || block.compare(Block.REDSTONE_WIRE)
        }
}