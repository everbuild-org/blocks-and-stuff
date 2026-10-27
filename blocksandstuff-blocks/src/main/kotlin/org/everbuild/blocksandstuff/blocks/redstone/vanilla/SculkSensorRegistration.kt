package org.everbuild.blocksandstuff.blocks.redstone.vanilla

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityDamageEvent
import net.minestom.server.event.entity.EntityDeathEvent
import net.minestom.server.event.entity.EntityDespawnEvent
import net.minestom.server.event.entity.EntityShootEvent
import net.minestom.server.event.entity.EntitySpawnEvent
import net.minestom.server.event.entity.EntityTeleportEvent
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.item.EntityEquipEvent
import net.minestom.server.event.item.PlayerFinishItemUseEvent
import net.minestom.server.event.player.PlayerEntityInteractEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent
import net.minestom.server.event.instance.InstanceUnregisterEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerMoveEvent
import org.everbuild.blocksandstuff.redstone.VibrationBus
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SculkSensorRegistration {
    private const val FREQUENCY_STEP = 1
    private const val FREQUENCY_SWIM = 2
    private const val FREQUENCY_HIT_GROUND = 2
    private const val FREQUENCY_ITEM_INTERACT_FINISH = 3
    private const val FREQUENCY_PROJECTILE_LAND = 2
    private const val FREQUENCY_PROJECTILE_SHOOT = 3
    private const val FREQUENCY_UNEQUIP = 4
    private const val FREQUENCY_ELYTRA_GLIDE = 4
    private const val FREQUENCY_EQUIP = 5
    private const val FREQUENCY_ENTITY_INTERACT = 6
    private const val FREQUENCY_ENTITY_DAMAGE = 7
    private const val FREQUENCY_EAT_DRINK = 8
    private const val FREQUENCY_BLOCK_DESTROY = 12
    private const val FREQUENCY_BLOCK_PLACE = 13
    private const val FREQUENCY_TELEPORT = 14
    private const val FREQUENCY_LIGHTNING_STRIKE = 14
    private const val FREQUENCY_ENTITY_DIE = 15

    private val lastStepPositions = ConcurrentHashMap<UUID, Point>()
    private val airborneItems = ConcurrentHashMap<Int, Boolean>()
    private var registeredHandler: Any? = null

    @JvmStatic
    fun registerDefault() {
        val handler = MinecraftServer.getGlobalEventHandler()
        if (registeredHandler === handler) return
        registeredHandler = handler

        handler.addListener(InstanceUnregisterEvent::class.java) { event ->
            VibrationBus.clear(event.instance)
        }
        handler.addListener(PlayerBlockPlaceEvent::class.java) { event ->
            if (isWoolOrCarpet(event.block)) return@addListener
            VibrationBus.emit(event.instance, event.blockPosition, FREQUENCY_BLOCK_PLACE)
        }
        handler.addListener(PlayerBlockBreakEvent::class.java) { event ->
            if (isWoolOrCarpet(event.block)) return@addListener
            VibrationBus.emit(event.instance, event.blockPosition, FREQUENCY_BLOCK_DESTROY)
        }
        handler.addListener(PlayerMoveEvent::class.java) { event ->
            val player = event.player
            val instance = player.instance ?: return@addListener
            val position = event.newPosition.asBlockVec()
            if (instance.getBlock(position).compare(Block.SCULK_SHRIEKER)) {
                VibrationBus.emit(instance, position, FREQUENCY_STEP)
            }
            if (player.isSneaking) return@addListener
            if (lastStepPositions.put(player.uuid, position) == position) return@addListener

            val frequency =
                when {
                    player.isFlyingWithElytra -> FREQUENCY_ELYTRA_GLIDE
                    player.isFlying -> return@addListener
                    player.entityMeta.isSwimming -> FREQUENCY_SWIM
                    !event.isOnGround -> return@addListener
                    else -> FREQUENCY_STEP
                }
            if (isWoolOrCarpet(instance.getBlock(position.sub(0.0, 1.0, 0.0)))) return@addListener
            VibrationBus.emit(instance, position, frequency)
        }
        handler.addListener(PlayerDisconnectEvent::class.java) { event ->
            lastStepPositions.remove(event.player.uuid)
        }
        handler.addListener(EntityDamageEvent::class.java) { event ->
            val instance = event.entity.instance ?: return@addListener
            VibrationBus.emit(instance, event.entity.position.asBlockVec(), FREQUENCY_ENTITY_DAMAGE)
        }
        handler.addListener(EntityTickEvent::class.java) { event ->
            val entity = event.entity
            if (entity.entityType != EntityType.ITEM) return@addListener
            val wasAirborne = airborneItems.put(entity.entityId, !entity.isOnGround)
            if (!entity.isOnGround || wasAirborne != true) return@addListener
            val instance = entity.instance ?: return@addListener
            if (isWoolOrCarpet(instance.getBlock(entity.position.asBlockVec().sub(0.0, 1.0, 0.0)))) return@addListener
            VibrationBus.emit(instance, entity.position.asBlockVec(), FREQUENCY_HIT_GROUND)
        }
        handler.addListener(EntityDespawnEvent::class.java) { event ->
            airborneItems.remove(event.entity.entityId)
        }
        handler.addListener(EntityDeathEvent::class.java) { event ->
            val instance = event.entity.instance ?: return@addListener
            VibrationBus.emit(instance, event.entity.position.asBlockVec(), FREQUENCY_ENTITY_DIE)
        }
        handler.addListener(EntityEquipEvent::class.java) { event ->
            val instance = event.entity.instance ?: return@addListener
            val frequency = if (event.equippedItem.isAir) FREQUENCY_UNEQUIP else FREQUENCY_EQUIP
            VibrationBus.emit(instance, event.entity.position.asBlockVec(), frequency)
        }
        handler.addListener(EntityTeleportEvent::class.java) { event ->
            val instance = event.entity.instance ?: return@addListener
            VibrationBus.emit(instance, event.newPosition.asBlockVec(), FREQUENCY_TELEPORT)
        }
        handler.addListener(PlayerEntityInteractEvent::class.java) { event ->
            VibrationBus.emit(event.instance, event.target.position.asBlockVec(), FREQUENCY_ENTITY_INTERACT)
        }
        handler.addListener(PlayerFinishItemUseEvent::class.java) { event ->
            val eating =
                event.itemStack.get(DataComponents.FOOD) != null ||
                    event.itemStack.get(DataComponents.CONSUMABLE) != null
            if (!eating && event.player.isSneaking) return@addListener
            VibrationBus.emit(
                event.instance,
                event.player.position.asBlockVec(),
                if (eating) FREQUENCY_EAT_DRINK else FREQUENCY_ITEM_INTERACT_FINISH,
            )
        }
        handler.addListener(ProjectileCollideWithBlockEvent::class.java) { event ->
            VibrationBus.emit(event.instance, event.collisionPosition.asBlockVec(), FREQUENCY_PROJECTILE_LAND)
        }
        handler.addListener(EntityShootEvent::class.java) { event ->
            val shooter = event.entity
            if (shooter is Player && shooter.isSneaking) return@addListener
            val projectile = event.projectile
            val instance = projectile.instance ?: shooter.instance ?: return@addListener
            VibrationBus.emit(instance, projectile.position.asBlockVec(), FREQUENCY_PROJECTILE_SHOOT)
        }
        handler.addListener(EntitySpawnEvent::class.java) { event ->
            if (event.entity.entityType == EntityType.LIGHTNING_BOLT) {
                VibrationBus.emit(event.instance, event.entity.position.asBlockVec(), FREQUENCY_LIGHTNING_STRIKE)
            }
        }
    }

    private fun isWoolOrCarpet(block: Block): Boolean {
        val key = block.key().value()
        return key.contains("wool") || key.contains("carpet")
    }
}
