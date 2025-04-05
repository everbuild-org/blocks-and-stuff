package org.everbuild.blocksandstuff.fluids

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.utils.Direction

class EmptyFluid : Fluid(Block.AIR, Material.BUCKET) {
     override fun canBeReplacedWith(
         instance: Instance?,
         point: Point?,
         other: Fluid?,
         direction: Direction?
    ): Boolean {
        return true
    }

    override fun getNextTickDelay(instance: Instance?, point: Point?, block: Block?): Int {
        return -1
    }

    override val isEmpty: Boolean
        get() = true

    override val blastResistance: Double
        get() = 0.0

    override fun getHeight(block: Block?, instance: Instance?, point: Point?): Double {
        return 0.0
    }

    override fun getHeight(block: Block?): Double {
        return 0.0
    }
}
