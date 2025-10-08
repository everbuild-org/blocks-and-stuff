package org.everbuild.blocksandstuff.blocks.placement

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.RegistryTag
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.tag.BlockTags

class TorchPlacementRule(block: Block) : BlockPlacementRule(block) {
    private val tagManager = Block.staticRegistry()
    private val fences = tagManager.getTag(Key.key("minecraft:fences"))!!
    private val walls = tagManager.getTag(Key.key("minecraft:walls"))!!
    private val glassPanes = BlockTags.getTaggedWith("blocksandstuff:glass_panes")
    private val nonFullButPlaceable = RegistryTag.direct(
        walls.toList()
                + fences.toList()
                + glassPanes.toList()
                + listOf(Block.IRON_BARS)
    )

    private fun getIsNotFullFace(instance: Block.Getter, position: Point, face: BlockFace): Boolean {
        return !instance.getBlock(position).registry()!!.collisionShape().isFaceFull(face)
    }

    private fun canSupportTorch(instance: Block.Getter, position: Point, blockFace: BlockFace): Boolean {
        val block = instance.getBlock(position)
        val isFullFace = !getIsNotFullFace(instance, position, blockFace)
        return isFullFace || (blockFace != BlockFace.TOP && nonFullButPlaceable.contains(block))
    }

    override fun blockPlace(placementState: PlacementState): Block? {
        var blockFace = placementState.blockFace() ?: return null
        var supporting = placementState.placePosition.add(blockFace.oppositeFace.toDirection().vec())
        val isNotFullFace = getIsNotFullFace(placementState.instance, supporting, blockFace)

        if (blockFace == BlockFace.BOTTOM) return null

        if (isNotFullFace && blockFace != BlockFace.TOP) {
            // placing on the side of a block with bottom support places the torch next to the block
            blockFace = BlockFace.TOP
            supporting = placementState.placePosition.add(0.0, -1.0, 0.0)
        }

        if (blockFace == BlockFace.TOP) {
            if (!canSupportTorch(placementState.instance, supporting, blockFace)) return null
            return block
        }

        val torch = when (placementState.block.registry()!!.material()) {
            Block.TORCH.registry()!!.material() -> Block.WALL_TORCH
            Block.SOUL_TORCH.registry()!!.material() -> Block.SOUL_WALL_TORCH
            Block.REDSTONE_TORCH.registry()!!.material() -> Block.REDSTONE_WALL_TORCH
            Block.COPPER_TORCH.registry()!!.material() -> Block.COPPER_WALL_TORCH
            else -> return null
        }

        return torch.withNbt(placementState.block.nbtOrEmpty())
            .withProperty("facing", placementState.blockFace!!.name.lowercase())
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        val supporting = updateState.currentBlock.getProperty("facing")
            ?.let { BlockFace.valueOf(it.uppercase()).oppositeFace }
                ?: BlockFace.BOTTOM

        if (!canSupportTorch(
                updateState.instance,
                updateState.blockPosition.add(supporting.toDirection().vec()),
                supporting
            )
        ) {
            DroppedItemFactory.maybeDrop(updateState)
            return Block.AIR
        }

        return updateState.currentBlock
    }
}