package org.everbuild.blocksandstuff.fluids.impl

import net.minestom.server.MinecraftServer
import net.minestom.server.ServerFlag
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.utils.Direction
import net.minestom.server.world.DimensionType
import org.everbuild.blocksandstuff.fluids.relativeTicks

open class LavaFluid(defaultBlock: Block, bucket: Material) : FlowableFluid(defaultBlock, bucket) {
    override val isInfinite: Boolean
        get() = false

    override fun getHoleRadius(instance: Instance?): Int {
        return 4
    }

    override fun getLevelDecreasePerBlock(instance: Instance?): Int {
        return 2
    }

    override fun getNextTickDelay(
        instance: Instance,
        point: Point,
        block: Block
    ): Int {
        return if(MinecraftServer.getDimensionTypeRegistry()[instance.dimensionType]!!.ultrawarm()) {
            10.relativeTicks
        } else {
            15.relativeTicks
        }
    }

    override fun getHeight(block: Block?, instance: Instance?, point: Point?): Double {
        TODO("Not yet implemented")
    }

    override val blastResistance: Double
        get() = 100.0

    override fun canBeReplacedWith(
        instance: Instance?,
        point: Point?,
        other: Fluid?,
        direction: Direction?
    ): Boolean {

        return direction == Direction.DOWN && this === other
    }
}
