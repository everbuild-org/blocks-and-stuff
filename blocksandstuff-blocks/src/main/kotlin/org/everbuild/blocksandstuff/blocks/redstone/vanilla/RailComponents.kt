package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext
import java.util.ArrayDeque

/**
 * Powered rail. Directly powered by redstone and propagates activation along connected powered
 * rails up to eight blocks away, like vanilla.
 */
class PoweredRailComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val chain = collectChain(ctx)
        val chainSet = chain.map { it.asBlockVec() }.toSet()
        val powered = HashSet<Point>()
        val queue = ArrayDeque<Pair<Point, Int>>()

        for (position in chain) {
            if (isPowered(ctx, position)) {
                val key = position.asBlockVec()
                powered.add(key)
                queue.add(key to 0)
            }
        }
        while (queue.isNotEmpty()) {
            val (position, distance) = queue.removeFirst()
            if (distance >= MAX_PROPAGATION) continue
            for (neighbour in horizontalRailNeighbours(chainSet, position)) {
                if (!powered.add(neighbour)) continue
                queue.add(neighbour to distance + 1)
            }
        }

        for (position in chain) {
            val block = ctx.block(position)
            ctx.setState(position, block.withProperty("powered", powered.contains(position.asBlockVec()).toString()))
        }
        return ComponentEvaluation.None
    }

    private fun collectChain(ctx: ReadContext): List<Point> {
        val result = ArrayList<Point>()
        val seen = HashSet<Point>()
        val queue = ArrayDeque<Point>()
        queue.add(ctx.position)
        seen.add(ctx.position)

        while (queue.isNotEmpty() && result.size < MAX_CHAIN_LENGTH) {
            val position = queue.removeFirst()
            result.add(position)
            for (face in HORIZONTAL_FACES) {
                val neighbour = position.relative(face)
                if (!isPoweredRail(ctx.block(neighbour))) continue
                if (seen.add(neighbour)) queue.add(neighbour)
            }
        }
        return result
    }

    private fun horizontalRailNeighbours(
        chain: Set<Point>,
        position: Point,
    ): List<Point> =
        HORIZONTAL_FACES
            .map { position.relative(it).asBlockVec() }
            .filter { it in chain }

    private fun isPoweredRail(block: Block): Boolean = block.compare(Block.POWERED_RAIL)

    private companion object {
        private const val MAX_PROPAGATION = 8
        private const val MAX_CHAIN_LENGTH = 64
        private val HORIZONTAL_FACES = BlockFace.entries.filter { it.toDirection().horizontal() }
    }
}

/** Detector rail. Powers redstone while a minecart is on top of it. */
class DetectorRailComponent : RedstoneComponent {
    private var nextCheckAt = -1L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val now = ctx.tick()
        if (now >= nextCheckAt) {
            nextCheckAt = now + CHECK_INTERVAL
            ctx.scheduleTick(CHECK_INTERVAL)
        }

        val block = ctx.block()
        val powered = hasMinecart(ctx.instance, ctx.position)
        if ((block.getProperty("powered") == "true") == powered) return ComponentEvaluation.None
        return ComponentEvaluation.SetState(block.withProperty("powered", powered.toString()))
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit = if (ctx.block().getProperty("powered") == "true") Emit(REDSTONE_MAX_POWER) else Emit.ZERO

    private fun hasMinecart(
        instance: Instance,
        position: Point,
    ): Boolean {
        val minX = position.x()
        val minY = position.y()
        val minZ = position.z()
        val maxX = minX + 1.0
        val maxY = minY + 1.0
        val maxZ = minZ + 1.0

        return instance.entities.any { entity ->
            if (!entity.entityType.key().value().endsWith("minecart")) return@any false
            val box = entity.boundingBox
            val origin = entity.position
            val entityMinX = origin.x() + box.minX()
            val entityMaxX = origin.x() + box.maxX()
            val entityMinY = origin.y() + box.minY()
            val entityMaxY = origin.y() + box.maxY()
            val entityMinZ = origin.z() + box.minZ()
            val entityMaxZ = origin.z() + box.maxZ()

            entityMaxX > minX && entityMinX < maxX &&
                entityMaxY > minY && entityMinY < maxY &&
                entityMaxZ > minZ && entityMinZ < maxZ
        }
    }

    private companion object {
        private const val CHECK_INTERVAL = 1
    }
}

/** Activator rail. Mirrors redstone power in its `powered` block state. */
class ActivatorRailComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val powered = isPowered(ctx)
        if ((block.getProperty("powered") == "true") == powered) return ComponentEvaluation.None
        return ComponentEvaluation.SetState(block.withProperty("powered", powered.toString()))
    }
}
