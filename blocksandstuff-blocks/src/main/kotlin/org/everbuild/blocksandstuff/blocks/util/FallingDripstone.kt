package org.everbuild.blocksandstuff.blocks.util

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.FallingBlockMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class FallingDripstone private constructor(
    private val block: Block,
) : Entity(EntityType.FALLING_BLOCK) {
    init {
        editEntityMeta(FallingBlockMeta::class.java) {
            it.block = block
        }
    }

    override fun tick(time: Long) {
        super.tick(time)
        if (!isOnGround) return

        val instance = instance ?: return
        val landingPosition = position.asBlockVec()
        block.material()?.let {
            DroppedItemFactory.maybeDrop(instance, landingPosition, ItemStack.of(it))
        }
        instance.playSoundExcept(
            null,
            Sound.sound(SoundEvent.BLOCK_POINTED_DRIPSTONE_LAND, Sound.Source.BLOCK, 1.0f, 1.0f),
            position,
        )
        remove()
    }

    companion object {
        fun spawn(
            block: Block,
            instance: Instance,
            pos: BlockVec,
        ) = FallingDripstone(block).also {
            it.setInstance(instance, pos.add(0.5, 0.0, 0.5))
        }
    }
}
