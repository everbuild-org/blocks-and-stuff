package org.everbuild.blocksandstuff.blocks.util

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.other.FallingBlockMeta
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

class FallingBlock private constructor(val block: Block) : Entity(EntityType.FALLING_BLOCK) {
    init {
        editEntityMeta(FallingBlockMeta::class.java) {
            it.block = block
        }
    }

    override fun tick(time: Long) {
        super.tick(time)
        if(this.isOnGround) {
            this.instance.setBlock(this.position.asBlockVec(), block)
            this.remove()
        }
    }

    companion object {
        fun spawn(block: Block, instance: Instance, pos: BlockVec) = FallingBlock(block).also {
            it.setInstance(instance, pos.add(0.5, 0.0, 0.5))
        }
    }
}