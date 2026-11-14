package org.everbuild.blocksandstuff.blocks.behavior

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.sound.SoundEvent
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class EnderChestHandler : BlockHandler {
    override fun getKey(): Key = KEY

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) return true

        ensureDisconnectListener()

        val player = interaction.player
        locations[player.uuid] = interaction.instance to interaction.blockPosition

        val existing = sessions[player.uuid]
        if (existing != null) {
            if (existing.ready) player.openInventory(existing.inventory)
            return false
        }

        val inventory = Inventory(InventoryType.CHEST_3_ROW, Component.translatable("container.enderchest"))
        val session = Session(inventory, player.getTag(ENDER_CHEST_PERSISTENCE))
        sessions[player.uuid] = session

        inventory.eventNode().addListener(InventoryOpenEvent::class.java) { handleOpen(it.player) }
        inventory.eventNode().addListener(InventoryCloseEvent::class.java) { handleClose(it.player) }

        load(session, player)
        return false
    }

    private fun load(
        session: Session,
        player: Player,
    ) {
        val persistence = session.persistence
        if (persistence == null) {
            session.ready = true
            player.openInventory(session.inventory)
            return
        }

        scope.launch {
            val result = runCatching { persistence.load(player.uuid) }
            withContext(serverDispatcher) {
                if (sessions[player.uuid] !== session || player.isRemoved) return@withContext

                result
                    .onSuccess { applyItems(session.inventory, it) }
                    .onFailure { error ->
                        MinecraftServer.LOGGER.warn("Ender chest load failed for ${player.uuid}", error)
                        session.persistence = null
                    }
                session.ready = true
                player.openInventory(session.inventory)
            }
        }
    }

    private fun handleOpen(player: Player) {
        val location = locations[player.uuid] ?: return
        playSound(location.first, location.second, SoundEvent.BLOCK_ENDER_CHEST_OPEN)
        location.first.sendBlockAction(location.second, LID_ACTION, 1.toByte())
    }

    private fun handleClose(player: Player) {
        locations.remove(player.uuid)?.let { location ->
            playSound(location.first, location.second, SoundEvent.BLOCK_ENDER_CHEST_CLOSE)
            location.first.sendBlockAction(location.second, LID_ACTION, 0.toByte())
        }

        sessions[player.uuid]?.let { persist(it, player) }
    }

    private fun applyItems(
        inventory: Inventory,
        items: List<ItemStack>,
    ) {
        for (slot in 0 until inventory.size) {
            inventory.setItemStack(slot, items.getOrElse(slot) { ItemStack.AIR })
        }
    }

    private fun playSound(
        instance: Instance,
        position: Point,
        event: SoundEvent,
    ) {
        instance.playSoundExcept(null, Sound.sound(event, Sound.Source.BLOCK, 0.5f, 1.0f), position)
    }

    private class Session(
        val inventory: Inventory,
        var persistence: EnderChestPersistence?,
        var ready: Boolean = false,
    )

    companion object {
        private val KEY = Key.key("minecraft:ender_chest")
        private const val LID_ACTION: Byte = 1

        private val sessions = HashMap<UUID, Session>()
        private val locations = HashMap<UUID, Pair<Instance, Point>>()

        internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        internal var serverDispatcher: CoroutineDispatcher = SchedulerDispatcher

        internal fun resetScheduling() {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            serverDispatcher = SchedulerDispatcher
        }

        private var disconnectListenerRegistered = false

        private fun ensureDisconnectListener() {
            if (disconnectListenerRegistered) return
            disconnectListenerRegistered = true

            MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent::class.java) { event ->
                val player = event.player
                locations.remove(player.uuid)
                sessions.remove(player.uuid)?.let { persist(it, player) }
            }
        }

        private fun persist(
            session: Session,
            player: Player,
        ) {
            if (!session.ready) return
            val persistence = session.persistence ?: return

            val items = (0 until session.inventory.size).map { session.inventory.getItemStack(it) }
            scope.launch {
                runCatching { persistence.save(player.uuid, items) }
                    .onFailure { MinecraftServer.LOGGER.warn("Ender chest save failed for ${player.uuid}", it) }
            }
        }
    }
}

private object SchedulerDispatcher : CoroutineDispatcher() {
    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(block)
    }
}
