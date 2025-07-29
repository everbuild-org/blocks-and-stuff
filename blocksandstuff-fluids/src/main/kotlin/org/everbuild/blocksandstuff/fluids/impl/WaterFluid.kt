package org.everbuild.blocksandstuff.fluids.impl

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.fluids.relativeTicks

open class WaterFluid(defaultBlock: Block, bucket: Material) : FlowableFluid(defaultBlock, bucket) {
    override val isInfinite: Boolean
        get() = true

    override fun getNextTickDelay(instance: Instance, point: Point, block: Block): Int = 5.relativeTicks

    override fun getHoleRadius(instance: Instance?): Int {
        return 4
    }

    override fun getLevelDecreasePerBlock(instance: Instance?): Int {
        return 1
    }

    override fun getHeight(block: Block?, instance: Instance?, point: Point?): Double {
        TODO("Not yet implemented")
    }

    override val blastResistance: Double
        get() = 100.0

    override fun canBeReplacedWith(instance: Instance?, point: Point?, other: Fluid?, direction: Direction?): Boolean {
        return direction == Direction.DOWN && this === other
    }

    override fun isInTile(block: Block): Boolean {
        return super.isInTile(block) || block.getProperty("waterlogged") == "true"
    }
}
