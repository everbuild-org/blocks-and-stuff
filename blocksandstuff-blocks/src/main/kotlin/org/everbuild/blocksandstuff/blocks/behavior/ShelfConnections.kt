package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace

internal object ShelfConnections {
    private const val MAX_CONNECTED_SHELVES = 3

    fun updateSideChain(
        instance: Block.Getter,
        position: Point,
        block: Block,
    ): Block {
        if (block.getProperty("side_chain") == null) return block
        val group = poweredGroup(instance, position, block)
        val index = group.indexOfFirst { it.first == position }
        val sideChain =
            when {
                group.size <= 1 -> "unconnected"
                index == 0 -> "left"
                index == group.lastIndex -> "right"
                else -> "center"
            }
        return if (block.getProperty("side_chain") == sideChain) block else block.withProperty("side_chain", sideChain)
    }

    fun poweredGroup(
        instance: Block.Getter,
        position: Point,
        block: Block,
    ): List<Pair<Point, Block>> {
        val facing = block.getProperty("facing") ?: return listOf(position to block)
        if (!isPoweredShelf(block, facing)) return listOf(position to block)

        val leftFace = leftFace(facing) ?: return listOf(position to block)
        val rightFace = leftFace.oppositeFace
        fun matchingShelf(at: Point): Block? =
            instance.getBlock(at).takeIf { isPoweredShelf(it, facing) }

        var first = position
        while (matchingShelf(first.relative(leftFace)) != null) {
            first = first.relative(leftFace)
        }

        var offset = 0
        var cursor = first
        while (cursor != position) {
            cursor = cursor.relative(rightFace)
            if (matchingShelf(cursor) == null) return listOf(position to block)
            offset++
        }

        repeat((offset / MAX_CONNECTED_SHELVES) * MAX_CONNECTED_SHELVES) {
            first = first.relative(rightFace)
        }

        return buildList {
            var member = first
            repeat(MAX_CONNECTED_SHELVES) {
                val shelf = matchingShelf(member) ?: return@buildList
                add(member to shelf)
                member = member.relative(rightFace)
            }
        }.ifEmpty { listOf(position to block) }
    }

    private fun isPoweredShelf(
        block: Block,
        facing: String,
    ): Boolean =
        block.getProperty("powered") == "true" &&
            block.getProperty("facing") == facing &&
            SHELF_BLOCKS.any { it.key() == block.key() }

    private fun leftFace(facing: String): BlockFace? =
        when (facing) {
            "north" -> BlockFace.EAST
            "east" -> BlockFace.SOUTH
            "south" -> BlockFace.WEST
            "west" -> BlockFace.NORTH
            else -> null
        }
}
