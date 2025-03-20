package org.everbuild.blocksandstuff.fluids

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.utils.Direction
import org.everbuild.averium.worlds.fluid.FlowableFluid
import org.everbuild.averium.worlds.fluid.Fluid

class LavaFluid(defaultBlock: Block, bucket: Material) : FlowableFluid(defaultBlock, bucket) {


    override val isInfinite: Boolean
        get() = false

    override fun onBreakingBlock(instance: Instance?, point: Point?, block: Block?): Boolean {
        val event = WaterBlockBreakEvent(instance, point, block)
        return !event.isCancelled
    }

    override fun getHoleRadius(instance: Instance?): Int {
        return 4
    }

    override fun getLevelDecreasePerBlock(instance: Instance?): Int {
        return 1
    }

    override fun getTickRate(instance: Instance?): Int {
        return 5 * (MinecraftServer.TICK_PER_SECOND / 20)
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
