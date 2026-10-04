package org.everbuild.blocksandstuff.blocks.placement

import java.util.Locale
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponents
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.rule.BlockPlacementRule
import net.minestom.server.network.player.ResolvableProfile
import org.everbuild.blocksandstuff.common.utils.sixteenStepRotation

class PlayerHeadPlacementRule(block: Block) : BlockPlacementRule(block) {
    override fun blockPlace(placementState: PlacementState): Block? {
        val clickedFace = placementState.blockFace ?: return block
        val nbt = placementState.usedItemStack?.get(DataComponents.PROFILE)?.let {
            val profile = ResolvableProfile.CODEC.encode(Transcoder.NBT, it).orElse(null) ?: return@let null
            CompoundBinaryTag.builder().put("profile", profile).build()
        }
        return when (clickedFace) {
            BlockFace.TOP, BlockFace.BOTTOM -> {
                val rotation = placementState.sixteenStepRotation()
                block.withProperty("rotation", rotation.toString())
                    .let {
                        if (nbt != null) it.withNbt(nbt) else it
                    }
            }
            else -> {
                val wallVariant = Block.PLAYER_WALL_HEAD
                val facing = clickedFace.name.lowercase(Locale.ROOT)
                wallVariant.withProperty("facing", facing)
                    .let {
                        if (nbt != null) it.withNbt(nbt) else it
                    }
            }
        }
    }

    override fun blockUpdate(updateState: UpdateState): Block {
        return updateState.currentBlock
    }
}