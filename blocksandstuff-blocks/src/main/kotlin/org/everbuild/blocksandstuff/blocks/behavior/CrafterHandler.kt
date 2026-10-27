package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.network.ConnectionState
import net.minestom.server.network.packet.client.play.ClientWindowSlotStatePacket
import net.minestom.server.network.packet.server.play.WindowPropertyPacket
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHandler
import org.everbuild.blocksandstuff.common.blockinventory.BlockInventoryHolder
import org.everbuild.blocksandstuff.common.blockinventory.SingleBlockInventoryBackend
import org.everbuild.blocksandstuff.common.blockinventory.insertStack
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import org.everbuild.blocksandstuff.common.utils.handlerOrRegistered
import org.everbuild.blocksandstuff.recipes.grid.CraftingGridRecipe
import org.everbuild.blocksandstuff.recipes.grid.GridPattern
import org.everbuild.blocksandstuff.recipes.grid.filterInvalid
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Crafter: crafts once per redstone pulse and ejects the result through its front. */
class CrafterHandler : BlockInventoryHandler(),
    BlockInventoryHolder {
    private var backend: SingleBlockInventoryBackend? = null
    private var changeListenerInstalled = false

    override fun getKey(): Key = KEY

    override fun getInventory(
        instance: Instance,
        blockPos: Point,
    ): SingleBlockInventoryBackend = backendOf(instance, blockPos)

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        if (interaction.player.isSneaking) return true

        val inventory = backendOf(interaction.instance, interaction.blockPosition).getViewableInventory()
        if (!changeListenerInstalled) {
            changeListenerInstalled = true
            inventory.eventNode().addListener(InventoryItemChangeEvent::class.java) {
                RedstoneRegistrations.notifyContainerChanged(interaction.instance, interaction.blockPosition)
            }
        }
        interaction.player.openInventory(inventory)
        (inventory as? CrafterInventory)?.refreshPreview()
        CrafterSessions.open(interaction.player, interaction.instance, interaction.blockPosition, inventory)
        return false
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        if (destroy.newBlock.key() == destroy.block.key()) return

        val destroyed = backendOf(destroy.instance, destroy.blockPosition).also { it.load(destroy.block) }
        BlockInventoryHandler.closeAndDrop(
            listOf(destroyed.getViewableInventory()),
            destroy.instance,
            destroy.blockPosition,
        )
        backend = null
    }

    fun craft(
        instance: Instance,
        position: Point,
    ): Boolean {
        val inventory = backendOf(instance, position)
        val disabled = CrafterSessions.disabledSlots(instance.getBlock(position))
        val pattern =
            GridPattern(
                List(3) { row ->
                    List(3) { column ->
                        val index = row * 3 + column
                        if (index in disabled) ItemStack.AIR else inventory.getItemStack(index)
                    }
                },
                nullValue = ItemStack.AIR,
            ).filterInvalid().minimizePattern()
        if (pattern.height == 0) {
            playSound(instance, position, SoundEvent.BLOCK_CRAFTER_FAIL)
            return false
        }

        val recipe =
            MinecraftServer
                .getRecipeManager()
                .recipes
                .filterIsInstance<CraftingGridRecipe>()
                .firstOrNull { it.matches(pattern) }
        if (recipe == null) {
            playSound(instance, position, SoundEvent.BLOCK_CRAFTER_FAIL)
            return false
        }

        val result = recipe.getResult(pattern)
        val remaining = recipe.takeOne(pattern) ?: return false
        val stacks = remaining.extendPattern(3, 3).grid.flatten().map { it ?: ItemStack.AIR }
        inventory.transact { set ->
            stacks.forEachIndexed { index, stack -> set(index, stack) }
        }

        eject(instance, position, instance.getBlock(position), result)
        RedstoneRegistrations.notifyContainerChanged(instance, position)
        playSound(instance, position, SoundEvent.BLOCK_CRAFTER_CRAFT)
        return true
    }

    private fun eject(
        instance: Instance,
        position: Point,
        block: Block,
        result: ItemStack,
    ) {
        val face = outputFace(block)
        val targetPosition = position.relative(face)
        val target = instance.getBlock(targetPosition).handlerOrRegistered() as? BlockInventoryHolder
        if (target != null) {
            val leftover = target.getInventory(instance, targetPosition).insertStack(result)
            if (leftover.isAir) return
            spawnItemEntity(instance, position, face, leftover)
            return
        }
        spawnItemEntity(instance, position, face, result)
    }

    private fun playSound(
        instance: Instance,
        position: Point,
        event: SoundEvent,
    ) {
        instance.playSoundExcept(
            null,
            Sound.sound(event, Sound.Source.BLOCK, 1.0f, 1.0f),
            position,
        )
    }

    private fun outputFace(block: Block): BlockFace {
        val front = (block.getProperty("orientation") ?: "north_up").substringBefore('_')
        return when (front) {
            "up" -> BlockFace.TOP
            "down" -> BlockFace.BOTTOM
            else -> BlockFace.valueOf(front.uppercase())
        }
    }

    private fun backendOf(
        instance: Instance,
        position: Point,
    ): SingleBlockInventoryBackend = backend ?: CrafterArchetype.createBackend(instance, position).also { backend = it }

    private companion object {
        private val KEY = Key.key("minecraft:crafter")
    }
}

internal object CrafterSessions {
    private data class Session(
        val instance: Instance,
        val position: Point,
        val inventory: Inventory,
        val disabled: MutableSet<Int>,
    )

    private val sessions = ConcurrentHashMap<UUID, Session>()
    private val DISABLED_SLOTS_TAG: Tag<List<Int>> = Tag.Integer("blocksandstuff:disabled_slots").list()
    private var registeredHandler: Any? = null

    fun registerDefault() {
        val handler = MinecraftServer.getGlobalEventHandler()
        if (registeredHandler === handler) return
        registeredHandler = handler

        MinecraftServer.getPacketListenerManager().setListener(
            ConnectionState.PLAY,
            ClientWindowSlotStatePacket::class.java,
        ) { packet, connection ->
            val player = connection.player ?: return@setListener
            handleSlotState(player, packet)
        }
        handler.addListener(InventoryCloseEvent::class.java) { event ->
            val session = sessions[event.player.uuid] ?: return@addListener
            if (session.inventory === event.inventory) sessions.remove(event.player.uuid)
        }
        handler.addListener(PlayerDisconnectEvent::class.java) { event ->
            sessions.remove(event.player.uuid)
        }
    }

    fun open(
        player: Player,
        instance: Instance,
        position: Point,
        inventory: Inventory,
    ) {
        val disabled = disabledSlots(instance.getBlock(position))
        val session = Session(instance, position, inventory, disabled.toMutableSet())
        sessions[player.uuid] = session
        for (slot in 0 until 9) {
            sendSlotState(player, session, slot)
        }
    }

    fun disabledSlots(block: Block): Set<Int> =
        block.getTag(DISABLED_SLOTS_TAG)?.filter { it in 0 until 9 }?.toSet() ?: emptySet()

    fun toggle(
        player: Player,
        slot: Int,
    ) {
        val session = sessions[player.uuid] ?: return
        if (slot !in 0 until 9) return
        setDisabled(player, session, slot, slot !in session.disabled)
    }

    private fun handleSlotState(
        player: Player,
        packet: ClientWindowSlotStatePacket,
    ) {
        val session = sessions[player.uuid] ?: return
        if (session.inventory.windowId.toInt() != packet.windowId) return
        if (packet.slot !in 0 until 9) return
        setDisabled(player, session, packet.slot, packet.newState)
    }

    private fun setDisabled(
        player: Player,
        session: Session,
        slot: Int,
        disabled: Boolean,
    ) {
        if (disabled) {
            session.disabled.add(slot)
        } else {
            session.disabled.remove(slot)
        }
        sendSlotState(player, session, slot)
        persist(session)
    }

    private fun sendSlotState(
        player: Player,
        session: Session,
        slot: Int,
    ) {
        player.sendPacket(
            WindowPropertyPacket(
                session.inventory.windowId.toInt(),
                slot.toShort(),
                (if (slot in session.disabled) 1 else 0).toShort(),
            ),
        )
    }

    private fun persist(session: Session) {
        val block = session.instance.getBlock(session.position)
        val updated = block.withTag(DISABLED_SLOTS_TAG, session.disabled.sorted())
        session.instance.setBlock(session.position, updated, false)
        RedstoneRegistrations.notifyContainerChanged(session.instance, session.position)
    }
}
