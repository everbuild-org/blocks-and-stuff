package org.everbuild.blocksandstuff.fluids.impl

import it.unimi.dsi.fastutil.shorts.Short2BooleanFunction
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap
import java.util.EnumMap
import kotlin.math.max
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Point
import net.minestom.server.event.EventDispatcher
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.Material
import net.minestom.server.utils.Direction
import org.everbuild.blocksandstuff.fluids.MinestomFluids
import org.everbuild.blocksandstuff.fluids.asBlockFace
import org.everbuild.blocksandstuff.fluids.event.BlockFluidReplacementEvent

abstract class FlowableFluid(defaultBlock: Block, bucket: Material) : Fluid(defaultBlock, bucket) {
    override fun onTick(instance: Instance, point: Point, block: Block) {
        var varBlock = block

        if (!isSource(varBlock)) {
            val updated = getUpdatedState(instance, point, varBlock)
            if (MinestomFluids.getFluidOnBlock(updated) == MinestomFluids.EMPTY) {
                varBlock = updated
                instance.setBlock(point, Block.AIR, true)
            } else if (updated !== varBlock) {
                varBlock = updated
                instance.setBlock(point, updated, true)
            }
        }
        tryFlow(instance, point, varBlock)
    }

    protected fun tryFlow(instance: Instance, point: Point, block: Block) {
        val fluid: Fluid = MinestomFluids.getFluidInstanceOnBlock(block)
        if (fluid == MinestomFluids.EMPTY) return

        val down = point.add(0.0, -1.0, 0.0)
        val downBlock = instance.getBlock(down)
        val updatedDownFluid = getUpdatedState(instance, down, downBlock)
        if (canFlow(instance, point, block, Direction.DOWN, down, downBlock, updatedDownFluid)) {
            flow(instance, down, downBlock, Direction.DOWN, updatedDownFluid)
            if (getAdjacentSourceCount(instance, point) >= 3) {
                flowSides(instance, point, block)
            }
        } else if (isSource(block) || !canFlowDown(instance, updatedDownFluid, point, block, down, downBlock)) {
            flowSides(instance, point, block)
        }
    }

    override fun isInTile(block: Block): Boolean {
        return block.compare(defaultBlock)
    }

    /**
     * Flows to the sides whenever possible, or to a hole if found
     */
    private fun flowSides(instance: Instance, point: Point, block: Block) {
        var newLevel: Int = getLevel(block) - getLevelDecreasePerBlock(instance)
        if (isFalling(block)) newLevel = 7
        if (newLevel <= 0) return

        val map = getSpread(instance, point, block)
        for ((direction, newBlock) in map) {
            val offset = point.add(
                direction.normalX().toDouble(),
                direction.normalY().toDouble(),
                direction.normalZ().toDouble()
            )
            val currentBlock = instance.getBlock(offset)
            if (!canFlow(instance, point, block, direction, offset, currentBlock, newBlock)) continue
            flow(instance, offset, currentBlock, direction, newBlock)
        }
    }

    /**
     * Gets the updated state of a source block by taking into account its surrounding blocks.
     */
    fun getUpdatedState(instance: Instance, point: Point, block: Block): Block {
        var highestLevel = 0
        var stillCount = 0
        for (direction in Direction.HORIZONTAL) {
            val directionPos = point.add(
                direction.normalX().toDouble(),
                direction.normalY().toDouble(),
                direction.normalZ().toDouble()
            )
            val directionBlock = instance.getBlock(directionPos)
            val directionFluid: Fluid = MinestomFluids.getFluidInstanceOnBlock(directionBlock)
            if (directionFluid !== this || !receivesFlow(
                    direction,
                    instance,
                    point,
                    block,
                    directionPos,
                    directionBlock
                )
            ) continue

            if (isSource(directionBlock)) {
                ++stillCount
            }
            highestLevel = max(highestLevel, getLevel(directionBlock)) // TODO: Remove if functional
        }

        if (isInfinite && stillCount >= 2) {
            // If there's 2 or more still fluid blocks around
            // and below is still or a solid block, make this block still
            val downBlock = instance.getBlock(point.add(0.0, -1.0, 0.0))
            if (downBlock.isSolid || isMatchingAndStill(downBlock)) {
                return getSource(false)
            }
        }

        val above = point.add(0.0, 1.0, 0.0)
        val aboveBlock = instance.getBlock(above)
        val aboveFluid: Fluid = MinestomFluids.getFluidInstanceOnBlock(aboveBlock)
        if (aboveFluid != MinestomFluids.EMPTY && aboveFluid == this && receivesFlow(
                Direction.UP,
                instance,
                point,
                block,
                above,
                aboveBlock
            )
        ) {
            return getFlowing(8, true)
        }

        val newLevel = highestLevel - getLevelDecreasePerBlock(instance)
        if (newLevel <= 0) return Block.AIR
        return getFlowing(newLevel, false)
    }

    private fun receivesFlow(
        face: Direction,
        instance: Instance,
        point: Point,
        block: Block,
        fromPoint: Point,
        fromBlock: Block
    ): Boolean {
        // Vanilla seems to check if the adjacent block shapes cover the same square, but this seems to work as well
        // (Might not work with some special blocks)
        // If there is anything wrong it is most likely this method :D

        return if (block.isLiquid) {
            when (face) {
                //return isSource(block) || getLevel(block) == 8;
                Direction.UP -> fromBlock.isLiquid || block.isSolid || block.isAir

                //return isSource(fromBlock) || getLevel(fromBlock) == 8;
                Direction.DOWN -> fromBlock.isLiquid || fromBlock.isSolid || fromBlock.isAir

                else -> true
            }
        } else {
            when (face) {
                Direction.UP -> block.isSolid || block.isAir
                Direction.DOWN -> block.isSolid || block.isAir
                else -> block.isSolid || block.isAir
            }
        }
    }

    /**
     * Returns a map with the directions the water can flow in and the block the water will become in that direction.
     * If a hole is found within `getHoleRadius()` blocks, the water will only flow in that direction.
     * A weight is used to determine which hole is the closest.
     */
    fun getSpread(instance: Instance, point: Point, block: Block): Map<Direction, Block> {
        var weight = 1000
        val map = EnumMap<Direction, Block>(
            Direction::class.java
        )
        val holeMap = Short2BooleanOpenHashMap()

        for (direction in Direction.HORIZONTAL) {
            val directionPoint = point.add(
                direction.normalX().toDouble(),
                direction.normalY().toDouble(),
                direction.normalZ().toDouble()
            )
            val directionBlock = instance.getBlock(directionPoint)
            val id = getID(point, directionPoint)

            val updatedBlock = getUpdatedState(instance, directionPoint, directionBlock)
            if (!canFlowThrough(
                    instance,
                    updatedBlock,
                    point,
                    block,
                    direction,
                    directionPoint,
                    directionBlock
                )
            ) continue

            val down = holeMap.computeIfAbsent(id, Short2BooleanFunction { _: Short ->
                val downPoint = directionPoint.add(0.0, -1.0, 0.0)
                canFlowDown(
                    instance, getFlowing(getLevel(updatedBlock), false),
                    directionPoint, directionBlock, downPoint, instance.getBlock(downPoint)
                )
            })

            val newWeight = if (down) 0 else getWeight(
                instance, directionPoint, 1,
                direction.opposite(), directionBlock, point, holeMap
            )
            if (newWeight < weight) map.clear()

            if (newWeight <= weight) {
                map[direction] = updatedBlock
                weight = newWeight
            }
        }
        return map
    }

    fun getWeight(
        instance: Instance, point: Point, initialWeight: Int, skipCheck: Direction,
        block: Block, originalPoint: Point, short2BooleanMap: Short2BooleanMap
    ): Int {
        var weight = 1000
        for (direction in Direction.HORIZONTAL) {
            if (direction == skipCheck) continue
            val directionPoint = point.add(
                direction.normalX().toDouble(),
                direction.normalY().toDouble(),
                direction.normalZ().toDouble()
            )
            val directionBlock = instance.getBlock(directionPoint)
            val id = getID(originalPoint, directionPoint)

            if (!canFlowThrough(
                    instance, getFlowing(getLevel(block), false), point, block,
                    direction, directionPoint, directionBlock
                )
            ) continue

            val down = short2BooleanMap.computeIfAbsent(id, Short2BooleanFunction { s: Short ->
                val downPoint = directionPoint.add(0.0, -1.0, 0.0)
                val downBlock = instance.getBlock(downPoint)
                canFlowDown(
                    instance, getFlowing(getLevel(block), false),
                    directionPoint, downBlock, downPoint, downBlock
                )
            })
            if (down) return initialWeight

            if (initialWeight < getHoleRadius(instance)) {
                val newWeight = getWeight(
                    instance, directionPoint, initialWeight + 1,
                    direction.opposite(), directionBlock, originalPoint, short2BooleanMap
                )
                if (newWeight < weight) weight = newWeight
            }
        }
        return weight
    }

    private fun getAdjacentSourceCount(instance: Instance, point: Point): Int {
        var i = 0
        for (direction in Direction.HORIZONTAL) {
            val currentPoint = point.add(
                direction.normalX().toDouble(),
                direction.normalY().toDouble(),
                direction.normalZ().toDouble()
            )
            val block = instance.getBlock(currentPoint)
            if (!isMatchingAndStill(block)) continue
            ++i
        }
        return i
    }

    /**
     * Returns whether the fluid can flow through a specific block
     */
    private fun canFill(instance: Instance, point: Point, block: Block, flowing: Block): Boolean {
        val event = BlockFluidReplacementEvent(instance, block, BlockVec(point))
        EventDispatcher.call(event)
        return block.isAir || block.registry().isReplaceable || !event.isCancelled
    }

    private fun canFlowDown(
        instance: Instance, flowing: Block, point: Point,
        block: Block, fromPoint: Point, fromBlock: Block
    ): Boolean {
        if (!getDirections(flowing).contains(Direction.DOWN)) return false
        if (!this.receivesFlow(Direction.DOWN, instance, point, block, fromPoint, fromBlock)) return false
        if (MinestomFluids.getFluidOnBlock(fromBlock) === this) return true
        return this.canFill(instance, fromPoint, fromBlock, flowing)
    }

    private fun canFlowThrough(
        instance: Instance, flowing: Block, point: Point, block: Block,
        face: Direction, fromPoint: Point, fromBlock: Block
    ): Boolean {
        if (!getDirections(flowing).contains(face)) return false
        return !isMatchingAndStill(fromBlock) && receivesFlow(face, instance, point, block, fromPoint, fromBlock)
                && canFill(instance, fromPoint, fromBlock, flowing)
    }

    protected fun canFlow(
        instance: Instance, fluidPoint: Point, flowingBlock: Block,
        flowDirection: Direction, flowTo: Point, flowToBlock: Block, newFlowing: Block
    ): Boolean {
        if (!getDirections(flowingBlock).contains(flowDirection)) return false

        return MinestomFluids.getFluidInstanceOnBlock(flowToBlock)
            .canBeReplacedWith(instance, flowTo, MinestomFluids.getFluidInstanceOnBlock(newFlowing), flowDirection)
                && receivesFlow(flowDirection, instance, fluidPoint, flowingBlock, flowTo, flowToBlock)
                && canFill(instance, flowTo, flowToBlock, newFlowing)
    }

    /**
     * Sets the position to the new block, executing `onBreakingBlock()` before breaking any non-air block.
     */
    protected fun flow(instance: Instance, point: Point, block: Block, direction: Direction?, newBlock: Block) {
        if (block == newBlock) return  // Prevent unnecessary updates

        //TODO waterloggable check

        if (point.y() >= instance.cachedDimensionType.minY()) instance.setBlock(point, newBlock)
    }

    private fun isMatchingAndStill(block: Block): Boolean {
        return MinestomFluids.getFluidOnBlock(block) === this && isSource(block)
    }

    fun getFlowing(level: Int, falling: Boolean): Block {
        return defaultBlock.withProperty("level", (if (falling) 8 else if (level == 0) 0 else 8 - level).toString())
    }

    fun getSource(falling: Boolean): Block {
        return if (falling) defaultBlock.withProperty("level", "8") else defaultBlock
    }

    protected abstract val isInfinite: Boolean

    protected abstract fun getLevelDecreasePerBlock(instance: Instance?): Int

    protected abstract fun getHoleRadius(instance: Instance?): Int

    override fun getHeight(block: Block?, instance: Instance?, point: Point?): Double {
        return if (isFluidAboveEqual(block!!, instance!!, point!!)) 1.0 else getHeight(block)
    }

    override fun getHeight(block: Block?): Double {
        return getLevel(block!!) / 9.0
    }

    protected open fun getDirections(block: Block): Collection<Direction> {
        if (block.isLiquid) return Direction.entries
        return Direction.entries.filter {
            !block.registry().collisionShape().isFaceFull(it.asBlockFace())
        }
    }

    companion object {
        /**
         * Creates a unique id based on the relation between point and point2
         */
        private fun getID(point: Point, point2: Point): Short {
            val i = (point2.x() - point.x()).toInt()
            val j = (point2.z() - point.z()).toInt()
            return ((i + 128 and 0xFF) shl 8 or (j + 128 and 0xFF)).toShort()
        }

        private fun isFluidAboveEqual(block: Block, instance: Instance, point: Point): Boolean {
            return MinestomFluids.getFluidOnBlock(block) === MinestomFluids.getFluidOnBlock(
                instance.getBlock(
                    point.add(
                        0.0,
                        1.0,
                        0.0
                    )
                )
            )
        }
    }
}
