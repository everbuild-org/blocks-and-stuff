package org.everbuild.averium.worlds.fluid

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.utils.Direction

abstract class Fluid(val defaultBlock: Block, bucket: Material) {
    val bucket: ItemStack = ItemStack.of(bucket)

    abstract fun canBeReplacedWith(
        instance: Instance?, point: Point?,
        other: Fluid?, direction: Direction?
    ): Boolean

    abstract fun getNextTickDelay(instance: Instance?, point: Point?, block: Block?): Int

    open fun onTick(instance: Instance, point: Point, block: Block) {}

    protected open val isEmpty: Boolean
        get() = false

    protected abstract val blastResistance: Double

    abstract fun getHeight(block: Block?, instance: Instance?, point: Point?): Double
    abstract fun getHeight(block: Block?): Double

    companion object {
        fun isSource(block: Block): Boolean {
            val levelStr = block.getProperty("level")
            return levelStr == null || levelStr.toInt() == 0
        }

        fun getLevel(block: Block): Int {
            val levelStr = block.getProperty("level") ?: return 8
            val level = levelStr.toInt()
            if (level >= 8) return 8 // Falling water

            return 8 - level
        }

        fun isFalling(block: Block): Boolean {
            val levelStr = block.getProperty("level") ?: return false
            return levelStr.toInt() >= 8
        }
    }
}
