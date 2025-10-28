package org.everbuild.blocksandstuff.blocks.placement

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.RegistryTag
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.tag.BlockTags

class TripwireHookPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val tagManager = Block.staticRegistry()
    private val fences = tagManager.getTag(Key.key("minecraft:fences"))!!
    private val walls = tagManager.getTag(Key.key("minecraft:walls"))!!
    private val glassPanes = BlockTags.getTaggedWith("blocksandstuff:glass_panes")
    private val nonFullButPlaceable =
        RegistryTag.direct(walls.toList() + fences.toList() + glassPanes.toList() + listOf(Block.IRON_BARS))

    override fun blockPlace(placementState: PlacementState): Block? {
        val facingBlock =
            placementState.instance.getBlock(placementState.placePosition.relative(placementState.blockFace()!!))
        val blockFace = placementState.blockFace() ?: return null
        val supporting = placementState.placePosition.add(blockFace.oppositeFace.toDirection().vec())
        val isNotFullFace = getIsNotFullFace(placementState.instance, supporting, blockFace)
        if (placementState.blockFace == BlockFace.TOP || placementState.blockFace == BlockFace.BOTTOM) return null
        if (isNotFullFace) return null
        return placementState.block
            .withProperty("facing", placementState.blockFace!!.name.lowercase())
            .withProperty(
                "attached",
                (facingBlock.compare(Block.TRIPWIRE) && canConnectWithTripwire(
                    placementState.instance,
                    placementState.placePosition,
                    blockFace
                )).toString()
            )
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val facing = BlockFace.valueOf(updateState.currentBlock.getProperty("facing")!!.uppercase())
        val facingBlock = updateState.instance.getBlock(updateState.blockPosition.relative(facing))
        val supporting = updateState.currentBlock.getProperty("facing")
            ?.let { BlockFace.valueOf(it.uppercase()).oppositeFace }
            ?: BlockFace.BOTTOM
        if (!canSupportTripwireHook(
                updateState.instance,
                updateState.blockPosition.add(supporting.toDirection().vec()),
                supporting
            )
        ) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }
        return if (facingBlock.compare(Block.TRIPWIRE) && canConnectWithTripwire(
                updateState.instance,
                updateState.blockPosition,
                facing
            )
        ) {
            updateState.currentBlock.withProperty("attached", "true")
        } else {
            updateState.currentBlock.withProperty("attached", "false")
        }
    }

    private fun getIsNotFullFace(instance: Block.Getter, position: Point, face: BlockFace): Boolean {
        return !instance.getBlock(position).registry()!!.collisionShape().isFaceFull(face)
    }

    private fun canSupportTripwireHook(instance: Block.Getter, position: Point, blockFace: BlockFace): Boolean {
        val block = instance.getBlock(position)
        val isFullFace = !getIsNotFullFace(instance, position, blockFace)
        return isFullFace || (blockFace != BlockFace.TOP && nonFullButPlaceable.contains(block))
    }

    private fun canConnectWithTripwire(instance: Block.Getter, position: Point, blockFace: BlockFace): Boolean {
        val direction = blockFace.toDirection().vec()
        var blockPosition = position.add(direction)
        repeat(40) {
            val block = instance.getBlock(blockPosition)
            if (block.compare(Block.TRIPWIRE_HOOK) && block.getProperty("facing") == blockFace.oppositeFace.name.lowercase()) return true
            if (!block.compare(Block.TRIPWIRE)) return false
            blockPosition = blockPosition.add(direction)
        }
        return false
    }
}