package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.common.utils.getAxis

/** Port of vanilla's [PistonStructureResolver]. */
class PistonStructureResolver(
    private val instance: Instance,
    private val pistonPos: Point,
    private val pistonDirection: BlockFace,
    private val extending: Boolean,
) {
    val pushDirection: BlockFace = if (extending) pistonDirection else pistonDirection.oppositeFace
    private val startPos: Point =
        if (extending) {
            pistonPos.relative(pistonDirection)
        } else {
            pistonPos.relative(pistonDirection).relative(pistonDirection)
        }

    val toPush: MutableList<Point> = mutableListOf()
    val toDestroy: MutableList<Point> = mutableListOf()

    fun resolve(): Boolean {
        toPush.clear()
        toDestroy.clear()

        val startBlock = instance.getBlock(startPos)
        if (!isPushable(startBlock, startPos, pushDirection, false, pistonDirection)) {
            if (extending && pushReaction(startBlock) == PushReaction.DESTROY) {
                toDestroy.add(startPos)
                return true
            }
            return false
        }
        if (!addBlockLine(startPos, pushDirection)) return false

        var index = 0
        while (index < toPush.size) {
            val position = toPush[index++]
            if (isSticky(instance.getBlock(position)) && !addBranchingBlocks(position)) return false
        }
        return true
    }

    private fun addBlockLine(
        initialPosition: Point,
        direction: BlockFace,
    ): Boolean {
        var position = initialPosition
        var block = instance.getBlock(position)
        if (block.air()) return true
        if (!isPushable(block, position, pushDirection, false, direction)) return true
        if (position == pistonPos) return true
        if (toPush.contains(position)) return true

        var lineLength = 1
        if (lineLength + toPush.size > MAX_PUSH_DEPTH) return false

        // Include the sticky blocks behind this one that are dragged along.
        while (isSticky(block)) {
            val behind = position.relative(pushDirection.oppositeFace)
            val behindBlock = instance.getBlock(behind)
            if (
                behindBlock.air() ||
                !canStickToEachOther(block, behindBlock) ||
                !isPushable(behindBlock, behind, pushDirection, false, pushDirection.oppositeFace) ||
                behind == pistonPos
            ) {
                break
            }
            position = behind
            block = behindBlock
            lineLength++
            if (lineLength + toPush.size > MAX_PUSH_DEPTH) return false
        }

        var added = 0
        for (step in lineLength - 1 downTo 0) {
            toPush.add(relative(position, pushDirection.oppositeFace, step))
            added++
        }

        var forwardSteps = 1
        while (true) {
            val forward = relative(position, pushDirection, forwardSteps)
            val collisionIndex = toPush.indexOf(forward)
            if (collisionIndex > -1) {
                reorderListAtCollision(added, collisionIndex)
                for (index in 0..collisionIndex + added) {
                    val pushed = toPush[index]
                    if (isSticky(instance.getBlock(pushed)) && !addBranchingBlocks(pushed)) return false
                }
                return true
            }

            block = instance.getBlock(forward)
            if (block.air()) return true
            if (!isPushable(block, forward, pushDirection, true, pushDirection) || forward == pistonPos) return false
            if (pushReaction(block) == PushReaction.DESTROY) {
                toDestroy.add(forward)
                return true
            }
            if (toPush.size >= MAX_PUSH_DEPTH) return false
            toPush.add(forward)
            added++
            forwardSteps++
        }
    }

    private fun reorderListAtCollision(
        added: Int,
        collisionIndex: Int,
    ) {
        val head = toPush.subList(0, collisionIndex).toList()
        val moved = toPush.subList(toPush.size - added, toPush.size).toList()
        val middle = toPush.subList(collisionIndex, toPush.size - added).toList()
        toPush.clear()
        toPush.addAll(head)
        toPush.addAll(moved)
        toPush.addAll(middle)
    }

    private fun addBranchingBlocks(position: Point): Boolean {
        val block = instance.getBlock(position)
        for (face in BlockFace.entries) {
            if (face.toDirection().getAxis() == pushDirection.toDirection().getAxis()) continue
            val neighbour = position.relative(face)
            val neighbourBlock = instance.getBlock(neighbour)
            if (canStickToEachOther(neighbourBlock, block) && !addBlockLine(neighbour, face)) return false
        }
        return true
    }

    private fun isPushable(
        block: Block,
        position: Point,
        pushDirection: BlockFace,
        allowDestroy: Boolean,
        pistonDirection: BlockFace,
    ): Boolean {
        if (block.air()) return true
        if (block.key() in immovableKeys) return false

        val dimension = MinecraftServer.getRegistries().dimensionType().get(instance.getDimensionType())
        if (dimension != null) {
            if (pushDirection == BlockFace.BOTTOM && position.blockY() <= dimension.minY()) return false
            if (pushDirection == BlockFace.TOP && position.blockY() >= dimension.maxY() - 1) return false
        }

        if (!block.compare(Block.PISTON) && !block.compare(Block.STICKY_PISTON)) {
            if (block.blockEntityType() != null) return false
            return when (pushReaction(block)) {
                PushReaction.BLOCK -> false
                PushReaction.DESTROY -> allowDestroy
                PushReaction.PUSH_ONLY -> pushDirection == pistonDirection
                PushReaction.NORMAL -> true
            }
        }

        return block.getProperty("extended") != "true"
    }

    private fun pushReaction(block: Block): PushReaction =
        when {
            block.key() in immovableKeys -> PushReaction.BLOCK
            block.blockEntityType() != null -> PushReaction.BLOCK
            block.key() in destroyOnPush -> PushReaction.DESTROY
            else -> PushReaction.NORMAL
        }

    private fun isSticky(block: Block): Boolean = block.key() == SLIME_BLOCK || block.key() == HONEY_BLOCK

    private fun canStickToEachOther(
        a: Block,
        b: Block,
    ): Boolean {
        if (a.key() == HONEY_BLOCK && b.key() == SLIME_BLOCK) return false
        if (a.key() == SLIME_BLOCK && b.key() == HONEY_BLOCK) return false
        if (doesNotStick(a) || doesNotStick(b)) return false
        return isSticky(a) || isSticky(b)
    }

    private fun doesNotStick(block: Block): Boolean =
        block.key() == Key.key("minecraft:heavy_core") ||
            block.key().value().endsWith("_glazed_terracotta")

    private fun relative(
        position: Point,
        face: BlockFace,
        steps: Int,
    ): Point {
        var result = position
        repeat(steps) { result = result.relative(face) }
        return result
    }

    private enum class PushReaction {
        NORMAL,
        DESTROY,
        BLOCK,
        PUSH_ONLY,
    }

    companion object {
        private const val MAX_PUSH_DEPTH = 12
        private val SLIME_BLOCK = Key.key("minecraft:slime_block")
        private val HONEY_BLOCK = Key.key("minecraft:honey_block")

        val immovableKeys: Set<Key> =
            setOf(
                Key.key("minecraft:bedrock"),
                Key.key("minecraft:obsidian"),
                Key.key("minecraft:crying_obsidian"),
                Key.key("minecraft:ancient_debris"),
                Key.key("minecraft:reinforced_deepslate"),
                Key.key("minecraft:enchanting_table"),
                Key.key("minecraft:beacon"),
                Key.key("minecraft:end_portal"),
                Key.key("minecraft:end_portal_frame"),
                Key.key("minecraft:ender_chest"),
                Key.key("minecraft:nether_portal"),
                Key.key("minecraft:respawn_anchor"),
                Key.key("minecraft:barrier"),
                Key.key("minecraft:command_block"),
                Key.key("minecraft:chain_command_block"),
                Key.key("minecraft:repeating_command_block"),
                Key.key("minecraft:structure_block"),
                Key.key("minecraft:jigsaw"),
                Key.key("minecraft:piston_head"),
                Key.key("minecraft:moving_piston"),
            )

        private val destroyOnPush: Set<Key> =
            (
                listOf(
                    Block.TORCH,
                    Block.WALL_TORCH,
                    Block.REDSTONE_TORCH,
                    Block.REDSTONE_WALL_TORCH,
                    Block.REDSTONE_WIRE,
                    Block.LEVER,
                    Block.TRIPWIRE,
                    Block.TRIPWIRE_HOOK,
                ) +
                    BlockTags.getTaggedWith("minecraft:buttons") +
                    BlockTags.getTaggedWith("minecraft:pressure_plates") +
                    BlockTags.getTaggedWith("minecraft:rails") +
                    BlockTags.getTaggedWith("minecraft:flowers") +
                    BlockTags.getTaggedWith("minecraft:saplings") +
                    BlockTags.getTaggedWith("minecraft:crops")
            ).map { it.key() }.toSet()
    }
}
