package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.EntityType
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.sound.SoundEvent
import org.everbuild.blocksandstuff.redstone.ComponentEvaluation
import org.everbuild.blocksandstuff.redstone.Emit
import org.everbuild.blocksandstuff.redstone.REDSTONE_MAX_POWER
import org.everbuild.blocksandstuff.redstone.ReadContext
import org.everbuild.blocksandstuff.redstone.RedstoneComponent
import org.everbuild.blocksandstuff.redstone.UpdateContext

/**
 * Tripwire hook. Scans up to 40 tripwire blocks in its facing direction for a second hook, keeps the
 * line attached and powers both hooks plus adjacent redstone while an entity touches the string.
 */
class TripwireHookComponent : RedstoneComponent {
    private var nextCheckAt = -1L

    override fun evaluate(ctx: UpdateContext): ComponentEvaluation {
        val block = ctx.block()
        val facing = Facing.of(block) ?: return ComponentEvaluation.None
        val now = ctx.tick()
        val wasAttached = block.getProperty("attached") == "true"
        val line = scanLine(ctx, facing)

        if (line != null) {
            var triggered = false
            for (segment in line.segments) {
                val segmentBlock = ctx.block(segment.position)
                val wasPowered = segmentBlock.getProperty("powered") == "true"
                val powered = !isDisarmed(segmentBlock) && hasEntity(ctx.instance, segment.position)
                if (powered) triggered = true
                if (powered != wasPowered) {
                    playSound(
                        ctx,
                        segment.position,
                        if (powered) SoundEvent.BLOCK_TRIPWIRE_CLICK_ON else SoundEvent.BLOCK_TRIPWIRE_CLICK_OFF,
                    )
                }
                update(
                    ctx,
                    segment.position,
                    segmentBlock
                        .withProperty("attached", "true")
                        .withProperty("powered", powered.toString()),
                )
            }
            if (!wasAttached) playSound(ctx, ctx.position, SoundEvent.BLOCK_TRIPWIRE_ATTACH)
            if (line.partnerBlock.getProperty("attached") != "true") {
                playSound(ctx, line.partnerPosition, SoundEvent.BLOCK_TRIPWIRE_ATTACH)
            }
            update(
                ctx,
                line.partnerPosition,
                line.partnerBlock
                    .withProperty("attached", "true")
                    .withProperty("powered", triggered.toString()),
            )
            update(
                ctx,
                ctx.position,
                block
                    .withProperty("attached", "true")
                    .withProperty("powered", triggered.toString()),
            )
            scheduleNextCheck(ctx, now, attached = true)
            return ComponentEvaluation.None
        }

        if (wasAttached) playSound(ctx, ctx.position, SoundEvent.BLOCK_TRIPWIRE_DETACH)
        update(
            ctx,
            ctx.position,
            block
                .withProperty("attached", "false")
                .withProperty("powered", "false"),
        )
        val adjacentWire = ctx.block(ctx.position.relative(facing)).compare(Block.TRIPWIRE)
        if (adjacentWire) scheduleNextCheck(ctx, now, attached = false)
        return ComponentEvaluation.None
    }

    override fun emit(
        ctx: ReadContext,
        face: BlockFace,
    ): Emit {
        val block = ctx.block()
        if (block.getProperty("powered") != "true") return Emit.ZERO
        val facing = Facing.of(block) ?: return Emit.ZERO
        val direct = if (face == facing.oppositeFace) REDSTONE_MAX_POWER else 0
        return Emit(REDSTONE_MAX_POWER, direct)
    }

    private fun scheduleNextCheck(
        ctx: UpdateContext,
        now: Long,
        attached: Boolean,
    ) {
        val delay = if (attached) ATTACHED_CHECK_INTERVAL else DETACHED_CHECK_INTERVAL
        if (now < nextCheckAt) return
        nextCheckAt = now + delay
        ctx.scheduleTick(delay)
    }

    private fun update(
        ctx: UpdateContext,
        position: Point,
        block: Block,
    ) {
        val current = ctx.block(position)
        if (current.stateId() == block.stateId() && current.nbt() == block.nbt()) return
        ctx.setState(position, block)
    }

    private fun playSound(
        ctx: UpdateContext,
        position: Point,
        event: SoundEvent,
    ) {
        ctx.instance.playSoundExcept(
            null,
            Sound.sound(event, Sound.Source.BLOCK, 0.4f, 0.6f),
            position,
        )
    }

    private fun scanLine(
        ctx: ReadContext,
        facing: BlockFace,
    ): Line? {
        val segments = ArrayList<Segment>(MAX_LINE_LENGTH)
        var position = ctx.position

        repeat(MAX_LINE_LENGTH + 1) {
            position = position.relative(facing)
            val block = ctx.block(position)
            when {
                block.compare(Block.TRIPWIRE) -> {
                    if (segments.size >= MAX_LINE_LENGTH) return null
                    segments.add(Segment(position))
                }

                block.compare(Block.TRIPWIRE_HOOK) -> {
                    if (segments.isEmpty()) return null
                    if (Facing.of(block) != facing.oppositeFace) return null
                    return Line(position, block, segments)
                }

                else -> return null
            }
        }
        return null
    }

    private fun isDisarmed(block: Block): Boolean = block.getProperty("disarmed") == "true"

    private fun hasEntity(
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
            if (entity.entityType == EntityType.AREA_EFFECT_CLOUD || entity.entityType == EntityType.MARKER) {
                return@any false
            }
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

    private data class Segment(
        val position: Point,
    )

    private data class Line(
        val partnerPosition: Point,
        val partnerBlock: Block,
        val segments: List<Segment>,
    )

    private companion object {
        private const val MAX_LINE_LENGTH = 40
        private const val ATTACHED_CHECK_INTERVAL = 1
        private const val DETACHED_CHECK_INTERVAL = 4
    }
}
