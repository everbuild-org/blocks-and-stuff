package org.everbuild.blocksandstuff.blocks.placement

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.registry.RegistryTag
import org.everbuild.blocksandstuff.common.tag.BlockTags
import org.everbuild.blocksandstuff.common.utils.sixteenStepRotation
import kotlin.collections.plus

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
    override fun blockPlace(placementState: PlacementState): Block? {
        val blockFace = placementState.blockFace() ?: return null
        val supporting = placementState.placePosition.add(blockFace.oppositeFace.toDirection().vec())
        val isNotFullFace = !placementState.instance.getBlock(supporting).registry().collisionShape().isFaceFull(blockFace)

        if (blockFace == BlockFace.BOTTOM) return null

        if (blockFace == BlockFace.TOP) {
            // Certain blocks like fences and walls don't have full faces on the top but torches can be placed on em
            if (isNotFullFace
                && !nonFullButPlaceable.contains(placementState.instance.getBlock(supporting))) return null
            return block
        }

        if (isNotFullFace) {
            return null
        }


        val torch = when (placementState.block.registry().material()) {
            Block.TORCH.registry().material() -> Block.WALL_TORCH
            Block.SOUL_TORCH.registry().material() -> Block.SOUL_WALL_TORCH
            Block.REDSTONE_TORCH.registry().material() -> Block.REDSTONE_WALL_TORCH
            else -> return null
        }

        return torch.withNbt(placementState.block.nbtOrEmpty())
            .withProperty("facing", placementState.blockFace!!.name.lowercase())
    }
}