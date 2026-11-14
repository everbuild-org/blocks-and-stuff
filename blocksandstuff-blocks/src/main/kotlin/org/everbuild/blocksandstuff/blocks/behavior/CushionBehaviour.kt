package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.MinecraftServer
import net.minestom.server.collision.BoundingBox
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

private val ANCHOR_SLICE = BoundingBox(1.0, 1.0 / 16.0, 1.0, Vec(0.0, 15.0 / 16.0, 0.0))
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

            val direction = event.blockFace.toDirection().vec()
            val adjacent = event.position.add(direction.x(), direction.y(), direction.z())
            val spawn =
                if (event.blockFace == net.minestom.server.instance.block.BlockFace.TOP) {
                    event.position.add(0.5, 1.0, 0.5)
                } else {
                    val target =
                        when {
                            isFree(instance, adjacent) -> adjacent
                            isFree(instance, event.position.add(0.0, 1.0, 0.0)) -> event.position.add(0.0, 1.0, 0.0)
                            else -> return@addListener
                        }
                    target.asVec().add(0.5, 0.0, 0.5)
                }
            var spawnPosition = spawn
            var attempts = 0
            while (attempts < 3 && !isFree(instance, spawnPosition.asBlockVec())) {
                spawnPosition = spawnPosition.add(0.0, 1.0, 0.0)
                attempts++
            }
            if (!isFree(instance, spawnPosition.asBlockVec())) return@addListener

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
                val y = parts[1].toIntOrNull() ?: return@forEach
                val z = parts[2].toIntOrNull() ?: return@forEach
                if ((x shr 4) != event.chunkX || (z shr 4) != event.chunkZ) return@forEach
                val color = runCatching { DyeColor.valueOf(parts[3].uppercase()) }.getOrNull() ?: return@forEach
                val cushion = CushionEntity(color)
                cushion.setInstance(event.instance, Pos(x + 0.5, y.toDouble(), z + 0.5))
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
        val below = instance.getBlock(position.sub(0.0, 1.0, 0.0))
        return !below.air() && !below.liquid()
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

    private fun isFree(
        instance: Instance,
        position: Point,
    ): Boolean {
        val block = instance.getBlock(position)
        return block.air() || block.replaceable()
    }

    private fun addRecord(
        instance: Instance,
        position: Point,
        color: DyeColor,
    ) {
        val record = "${position.blockX()},${position.blockY()},${position.blockZ()},${color.name.lowercase()}"
        val records = instance.getTag(CUSHIONS_TAG) ?: emptyList()
        if (record !in records) instance.setTag(CUSHIONS_TAG, records + record)
    }

    private fun removeRecord(
        instance: Instance,
        position: Point,
        color: DyeColor,
    ) {
        val record = "${position.blockX()},${position.blockY()},${position.blockZ()},${color.name.lowercase()}"
        val records = instance.getTag(CUSHIONS_TAG) ?: return
        instance.setTag(CUSHIONS_TAG, records - record)
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
