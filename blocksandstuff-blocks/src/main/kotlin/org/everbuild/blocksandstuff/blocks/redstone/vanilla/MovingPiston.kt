package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.nbt.CompoundBinaryTag
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Finalizes a `moving_piston` block after its two game tick movement. The final state is carried in
 * the vanilla moving-piston block entity NBT and mirrored to clients by Minestom.
 */
class MovingPistonComponent : RedstoneComponent {
    private var tickScheduled = false
    private var scheduledAt = 0L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val now = ctx.tick()
        if (!tickScheduled) {
            tickScheduled = true
            scheduledAt = now + FINALIZE_DELAY_TICKS
            ctx.scheduleTick(FINALIZE_DELAY_TICKS)
            return ComponentEvaluation.None
        }
        if (now < scheduledAt) return ComponentEvaluation.None

        val moved = movedState(ctx.block()) ?: Block.AIR
        ctx.setState(moved)
        if (ctx.block().stateId() != moved.stateId() || ctx.block().nbt() != moved.nbt()) {
            setBlockSilently(ctx.instance, ctx.position, moved)
            RedstoneRegistrations.notifyBlockChanged(ctx.instance, ctx.position)
        }
        return ComponentEvaluation.None
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit {
        val moved = movedState(ctx.block()) ?: return Emit.ZERO
        return if (moved.compare(Block.REDSTONE_BLOCK)) Emit(REDSTONE_MAX_POWER) else Emit.ZERO
    }

    private companion object {
        private const val FINALIZE_DELAY_TICKS = 1
    }
}

/** Server-side block write without sending block changes to clients. */
internal fun setBlockSilently(
    instance: Instance,
    position: Point,
    block: Block,
) {
    val chunk = instance.getChunkAt(position) ?: return
    chunk.lockWriteLock()
    try {
        chunk.setBlock(position.blockX() and 15, position.blockY(), position.blockZ() and 15, block)
    } finally {
        chunk.unlockWriteLock()
    }
}

internal fun movingPistonBlock(
    moved: Block,
    facing: BlockFace,
    extending: Boolean,
    source: Boolean,
    sticky: Boolean,
): Block =
    Block.MOVING_PISTON
        .withProperty("facing", facing.propertyValue())
        .withProperty("type", if (sticky) "sticky" else "normal")
        .withNbt(
            CompoundBinaryTag
                .builder()
                .put("blockState", blockStateNbt(moved))
                .putInt("facing", facing.dataValue())
                .putFloat("progress", 0.0f)
                .putByte("extending", if (extending) 1 else 0)
                .putByte("source", if (source) 1 else 0)
                .build(),
        )

internal fun movedState(block: Block): Block? {
    val nbt = block.nbt() ?: return null
    val state = nbt.getCompound("blockState")
    val name = state.getString("Name")
    if (name.isEmpty()) return null
    var moved = Block.fromKey(name) ?: return null
    val properties = state.getCompound("Properties")
    for (key in properties.keySet()) {
        moved = moved.withProperty(key, properties.getString(key))
    }
    return moved
}

private fun blockStateNbt(block: Block): CompoundBinaryTag {
    val builder = CompoundBinaryTag.builder().putString("Name", block.key().asString())
    if (block.properties().isNotEmpty()) {
        val properties = CompoundBinaryTag.builder()
        block.properties().forEach { (key, value) -> properties.putString(key, value) }
        builder.put("Properties", properties.build())
    }
    return builder.build()
}

internal fun BlockFace.propertyValue(): String =
    when (this) {
        BlockFace.TOP -> "up"
        BlockFace.BOTTOM -> "down"
        else -> name.lowercase()
    }

internal fun BlockFace.dataValue(): Int =
    when (this) {
        BlockFace.BOTTOM -> 0
        BlockFace.TOP -> 1
        BlockFace.NORTH -> 2
        BlockFace.SOUTH -> 3
        BlockFace.WEST -> 4
        BlockFace.EAST -> 5
    }
