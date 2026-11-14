package org.everbuild.blocksandstuff.blocks.util

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.FallingBlockMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory

class FallingBlock private constructor(
    val block: Block,
) : Entity(EntityType.FALLING_BLOCK) {
    init {
        editEntityMeta(FallingBlockMeta::class.java) {
            it.block = block
        }
    }

    override fun tick(time: Long) {
        super.tick(time)
        if (!this.isOnGround) return

        val instance = this.instance ?: run {
            remove()
            return
        }
        val position = this.position.asBlockVec()
        val at = instance.getBlock(position)
        val below = instance.getBlock(position.sub(0.0, 1.0, 0.0))
        val blocked = !at.air() && !at.replaceable()
        val supported = below.collisionShape().isFaceFull(BlockFace.TOP)

        if (blocked || !supported) {
            DroppedItemFactory.maybeDrop(instance, position, block)
        } else {
            instance.setBlock(position, block)
        }
        remove()
    }

    companion object {
        fun spawn(
            block: Block,
            instance: Instance,
            pos: BlockVec,
        ) = FallingBlock(block).also {
            it.setInstance(instance, pos.add(0.5, 0.0, 0.5))
        }
    }
}
