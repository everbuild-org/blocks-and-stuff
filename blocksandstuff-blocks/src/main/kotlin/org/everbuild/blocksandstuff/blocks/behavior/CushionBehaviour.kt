package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.collision.BoundingBox
import net.minestom.server.collision.ShapeImpl
import net.minestom.server.color.DyeColor
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.entity.metadata.other.CushionMeta
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.event.player.PlayerInputEvent
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import kotlin.math.abs
import kotlin.math.floor

class CushionEntity(
    val color: DyeColor,
) : Entity(EntityType.CUSHION) {
    private var ticksSinceLastCheck = 0

    init {
        editEntityMeta(CushionMeta::class.java) { it.color = color }
        setNoGravity(true)
        setBoundingBox(1.0, 0.25, 1.0)
    }

    override fun preventBlockPlacement(): Boolean = false

    override fun tick(time: Long) {
        super.tick(time)
        val currentInstance = instance ?: return
        if (ticksSinceLastCheck++ < CHECK_INTERVAL_TICKS) return
        ticksSinceLastCheck = 0
        if (!isSupported(currentInstance)) {
            CushionBehaviour.drop(this, currentInstance)
        }
    }

    private fun isSupported(instance: Instance): Boolean = CushionBehaviour.supports(instance, position)
}

private val CUSHION_BOUNDS = BoundingBox(1.0, 0.25, 1.0)
private const val SHAPE_EPSILON = 1.0E-6
private const val CHECK_INTERVAL_TICKS = 100

private val CUSHIONS_TAG: Tag<List<String>> = Tag.String("blocksandstuff:cushions").list()

object CushionBehaviour {
    private const val NODE_NAME = "blocksandstuff:cushions"

    fun enable() {
        val global = MinecraftServer.getGlobalEventHandler()
        global.removeChildren(NODE_NAME)
        val handler = EventNode.all(NODE_NAME)
        global.addChild(handler)
        handler.addListener(PlayerUseItemOnBlockEvent::class.java) { event ->
            val color = cushionColor(event.itemStack.material()) ?: return@addListener
            if (event.player.gameMode == GameMode.SPECTATOR) return@addListener
            val instance = event.player.instance ?: return@addListener

            val spawn = placementPosition(instance, event.position, event.cursorPosition, event.blockFace) ?: return@addListener
            val supportBlock = event.position.takeIf { event.blockFace == BlockFace.TOP }
            val spawnPosition = findFreePosition(instance, spawn, supportBlock) ?: return@addListener

            val cushion = CushionEntity(color)
            cushion.setInstance(instance, spawnPosition)
            addRecord(instance, spawnPosition, color)
            consume(event.player, event.hand, event.itemStack)
        }
        handler.addListener(PlayerEntityInteractEvent::class.java) { event ->
            val cushion = event.target as? CushionEntity ?: return@addListener
            val instance = cushion.instance ?: return@addListener
            val held = event.player.getItemInHand(event.hand)
            val block = held.material().block()

            if (block != null && !held.isAir) {
                val target = cushion.position.asBlockVec()
                val current = instance.getBlock(target)
                if (current.air() || current.replaceable()) {
                    instance.setBlock(target, block)
                    consume(event.player, event.hand, held)
                    return@addListener
                }
            }

            cushion.addPassenger(event.player)
        }
        handler.addListener(EntityAttackEvent::class.java) { event ->
            val cushion = event.target as? CushionEntity ?: return@addListener
            val instance = cushion.instance ?: return@addListener
            drop(cushion, instance)
        }
        handler.addListener(PlayerInputEvent::class.java) { event ->
            if (!event.hasPressedShiftKey()) return@addListener
            val cushion = event.player.vehicle as? CushionEntity ?: return@addListener
            cushion.removePassenger(event.player)
            dismountSafely(event.player, cushion.position)
        }
        handler.addListener(InstanceChunkLoadEvent::class.java) { event ->
            val records = event.instance.getTag(CUSHIONS_TAG) ?: return@addListener
            records.forEach { record ->
                val parts = record.split(",")
                if (parts.size != 4) return@forEach
                val x = parts[0].toIntOrNull() ?: return@forEach
                val y = parts[1].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return@forEach
                val z = parts[2].toIntOrNull() ?: return@forEach
                if ((x shr 4) != event.chunkX || (z shr 4) != event.chunkZ) return@forEach
                val color = runCatching { DyeColor.valueOf(parts[3].uppercase()) }.getOrNull() ?: return@forEach
                val cushion = CushionEntity(color)
                cushion.setInstance(event.instance, Pos(x + 0.5, y, z + 0.5))
            }
        }
        handler.addListener(InstanceTickEvent::class.java) { event ->
            if (event.instance.worldAge % CHECK_INTERVAL_TICKS != 0L) return@addListener
            event.instance.getEntities()
                .filter { it.entityType == EntityType.CUSHION && !it.isRemoved }
                .forEach { cushion ->
                    if (!supports(event.instance, cushion.position)) {
                        dropAny(cushion, event.instance)
                    }
                }
        }
    }

    internal fun supports(
        instance: Instance,
        position: Point,
    ): Boolean {
        val at = instance.getBlock(position)
        if (coversCushion(at)) return false
        val blockX = floor(position.x()).toInt()
        val blockZ = floor(position.z()).toInt()
        val blockY = floor(position.y()).toInt()
        for (supportY in blockY downTo blockY - 2) {
            val support = instance.getBlock(Vec(blockX.toDouble(), supportY.toDouble(), blockZ.toDouble()))
            if (hasSurfaceAt(support, position.y() - supportY)) return true
        }
        return false
    }

    private fun coversCushion(block: Block): Boolean {
        if (block.key().value().contains("copper_grate")) return false
        val shape = block.collisionShape()
        return shape.isFaceFull(BlockFace.TOP) && shape.isFaceFull(BlockFace.BOTTOM)
    }

    private fun dropAny(
        entity: Entity,
        instance: Instance,
    ) {
        if (entity is CushionEntity) {
            drop(entity, instance)
            return
        }
        val color = runCatching { (entity.entityMeta as? CushionMeta)?.color }.getOrNull() ?: DyeColor.WHITE
        val position = entity.position
        entity.remove()
        cushionItem(color)?.let {
            DroppedItemFactory.maybeDrop(instance, position, ItemStack.of(it))
        }
    }

    internal fun drop(
        cushion: CushionEntity,
        instance: Instance,
    ) {
        val passengers = cushion.passengers.filterIsInstance<Player>()
        removeRecord(instance, cushion.position, cushion.color)
        cushion.remove()
        passengers.forEach { dismountSafely(it, cushion.position) }
        cushionItem(cushion.color)?.let {
            DroppedItemFactory.maybeDrop(instance, cushion.position, ItemStack.of(it))
        }
    }

    internal fun placementPosition(
        instance: Instance,
        blockPosition: Point,
        cursorPosition: Point,
        blockFace: BlockFace,
    ): Point? {
        if (blockFace == BlockFace.TOP) {
            val block = instance.getBlock(blockPosition)
            val surface = surfaceHeightAt(block, cursorPosition.x(), cursorPosition.y(), cursorPosition.z()) ?: return null
            return Pos(blockPosition.blockX() + 0.5, blockPosition.blockY() + surface, blockPosition.blockZ() + 0.5)
        }

        val adjacent = blockPosition.add(blockFace.toDirection().vec())
        val target =
            when {
                isFree(instance, adjacent.asVec().add(0.5, 0.0, 0.5)) -> adjacent
                isFree(instance, blockPosition.add(0.0, 1.0, 0.0).asVec().add(0.5, 0.0, 0.5)) -> blockPosition.add(0.0, 1.0, 0.0)
                else -> return null
            }
        return target.asVec().add(0.5, 0.0, 0.5)
    }

    internal fun findFreePosition(
        instance: Instance,
        position: Point,
        supportBlockPosition: Point? = null,
    ): Point? {
        var candidate = position
        repeat(4) {
            val supportingBlock = supportBlockPosition.takeIf { candidate == position }
            if (isFree(instance, candidate, supportingBlock)) return candidate
            candidate = candidate.add(0.0, 1.0, 0.0)
        }
        return null
    }

    private fun surfaceHeightAt(
        block: Block,
        localX: Double,
        hitY: Double,
        localZ: Double,
    ): Double? {
        if (block.air() || block.liquid()) return null
        return collisionBoxes(block)
            .asSequence()
            .filter { localX >= it.minX() && localX <= it.maxX() && localZ >= it.minZ() && localZ <= it.maxZ() }
            .map { it.maxY() }.firstOrNull { abs(it - hitY) < SHAPE_EPSILON }
    }

    private fun hasSurfaceAt(
        block: Block,
        localY: Double,
    ): Boolean {
        if (block.air() || block.liquid()) return false
        return collisionBoxes(block).any { kotlin.math.abs(it.maxY() - localY) < SHAPE_EPSILON }
    }

    private fun collisionBoxes(block: Block): List<BoundingBox> {
        val shape = block.collisionShape()
        return when (shape) {
            is BoundingBox -> listOf(shape)
            is ShapeImpl -> shape.boundingBoxes()
            else -> listOf(BoundingBox.fromPoints(shape.relativeStart(), shape.relativeEnd()))
        }
    }

    private fun isFree(
        instance: Instance,
        position: Point,
        ignoredBlock: Point? = null,
    ): Boolean {
        val minX = position.x() - 0.5
        val maxX = position.x() + 0.5 - SHAPE_EPSILON
        val minY = position.y() + SHAPE_EPSILON
        val maxY = position.y() + CUSHION_BOUNDS.height() - SHAPE_EPSILON
        val minZ = position.z() - 0.5
        val maxZ = position.z() + 0.5 - SHAPE_EPSILON

        for (blockX in floor(minX).toInt()..floor(maxX).toInt()) {
            for (blockY in floor(minY).toInt()..floor(maxY).toInt()) {
                for (blockZ in floor(minZ).toInt()..floor(maxZ).toInt()) {
                    if (ignoredBlock != null &&
                        blockX == ignoredBlock.blockX() &&
                        blockY == ignoredBlock.blockY() &&
                        blockZ == ignoredBlock.blockZ()
                    ) {
                        continue
                    }
                    val block = instance.getBlock(Vec(blockX.toDouble(), blockY.toDouble(), blockZ.toDouble()))
                    if (block.air() || block.replaceable()) continue
                    val relativePosition = Vec(position.x() - blockX, position.y() - blockY + SHAPE_EPSILON, position.z() - blockZ)
                    if (block.collisionShape().intersectBox(relativePosition, CUSHION_BOUNDS)) return false
                }
            }
        }
        return true
    }

    private fun addRecord(
        instance: Instance,
        position: Point,
        color: DyeColor,
    ) {
        val record = cushionRecord(position, color)
        val records = instance.getTag(CUSHIONS_TAG) ?: emptyList()
        if (record !in records) instance.setTag(CUSHIONS_TAG, records + record)
    }

    private fun removeRecord(
        instance: Instance,
        position: Point,
        color: DyeColor,
    ) {
        val record = cushionRecord(position, color)
        val records = instance.getTag(CUSHIONS_TAG) ?: return
        instance.setTag(CUSHIONS_TAG, records - record)
    }

    private fun cushionRecord(
        position: Point,
        color: DyeColor,
    ): String {
        val y = if (position.y() % 1.0 == 0.0) position.blockY().toString() else position.y().toString()
        return "${position.blockX()},$y,${position.blockZ()},${color.name.lowercase()}"
    }

    private fun consume(
        player: Player,
        hand: PlayerHand,
        item: ItemStack,
    ) {
        if (player.gameMode == GameMode.CREATIVE) return
        val current = player.getItemInHand(hand)
        player.setItemInHand(hand, if (current.amount() <= 1) ItemStack.AIR else current.withAmount(current.amount() - 1))
    }

    private fun cushionColor(material: Material): DyeColor? {
        val value = material.key().value()
        if (!value.endsWith("_cushion")) return null
        val color = value.removeSuffix("_cushion").uppercase()
        return runCatching { DyeColor.valueOf(color) }.getOrNull()
    }

    internal fun isCushionItem(material: Material): Boolean = cushionColor(material) != null

    private fun cushionItem(color: DyeColor): Material? =
        Material.fromKey("minecraft:${color.name.lowercase()}_cushion")

    /**
     * Minestom dismounts towards the vehicle's origin, which can leave the player slightly inside the
     * block below a cushion. Lift the player to the cushion's surface before releasing them.
     */
    private fun dismountSafely(
        player: Player,
        cushionPosition: Point,
    ) {
        player.teleport(Pos(cushionPosition.x(), cushionPosition.y() + 0.1, cushionPosition.z(), player.position.yaw(), player.position.pitch()))
    }
}
