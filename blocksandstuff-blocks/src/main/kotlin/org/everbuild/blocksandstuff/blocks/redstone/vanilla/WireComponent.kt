package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.blocks.placement.WireConnections
import org.everbuild.blocksandstuff.redstone.ALL_FACES
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Redstone wire. Stores its strength in the `power` property and decays by one per wire step.
 */
class WireComponent : RedstoneComponent {
    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val connected = WireConnections.updatedWire(ctx.instance, ctx.position, block) ?: block
        val updated = connected.withProperty("power", computePower(ctx).toString())
        if (updated.stateId() == block.stateId() && updated.nbt() == block.nbt()) return ComponentEvaluation.None
        for (wirePosition in WireConnections.connectedWirePositions(ctx.instance, ctx.position)) {
            ctx.markDirty(wirePosition)
        }
        return ComponentEvaluation.SetState(updated)
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit {
        val block = ctx.block()
        val power = powerOf(block)
        if (power <= 0) return Emit.ZERO

        val hasConnection = HORIZONTAL_PROPERTIES.any { (block.getProperty(it) ?: "none") != "none" }
        val supported =
            ctx.block(ctx.position.sub(0.0, 1.0, 0.0)).collisionShape().isFaceFull(BlockFace.TOP)
        if (!hasConnection && !supported) return Emit(power)

        return when (face) {
            BlockFace.BOTTOM -> Emit(power)
            BlockFace.TOP -> Emit.ZERO
            else -> {
                val connection = block.getProperty(face.name.lowercase())
                if (connection == null || connection == "none") Emit.ZERO else Emit(power)
            }
        }
    }

    private fun computePower(ctx: ReadContext): Int {
        var best = 0
        for (wirePosition in WireConnections.connectedWirePositions(ctx.instance, ctx.position)) {
            val contribution = powerOf(ctx.block(wirePosition)) - 1
            if (contribution > best) best = contribution
        }
        for (face in ALL_FACES) {
            val neighbour = ctx.position.relative(face)
            val block = ctx.block(neighbour)
            if (isWire(block)) continue
            val into = face.oppositeFace
            val contribution = maxOf(ctx.power().weakSignal(neighbour, into), ctx.power().directSignal(neighbour, into))
            if (contribution > best) best = contribution
        }
        return best.coerceIn(0, REDSTONE_MAX_POWER)
    }

    companion object {
        private val HORIZONTAL_PROPERTIES = listOf("north", "east", "south", "west")

        fun isWire(block: Block): Boolean = block.compare(Block.REDSTONE_WIRE)

        fun powerOf(block: Block): Int = block.getProperty("power")?.toIntOrNull()?.coerceIn(0, REDSTONE_MAX_POWER) ?: 0
    }
}
