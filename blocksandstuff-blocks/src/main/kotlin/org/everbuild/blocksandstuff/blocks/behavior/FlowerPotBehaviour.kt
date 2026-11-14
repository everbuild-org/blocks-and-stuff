package org.everbuild.blocksandstuff.blocks.behavior

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.item.DroppedItemFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A plant that can be placed in an empty flower pot.
 *
 * [matches] selects the item that turns an empty flower pot into [pottedBlock]; [drop] is the item
 * returned when the plant is taken out or the potted block is destroyed.
 */
class CustomPlant(
    val pottedBlock: Block,
    val drop: ItemStack,
    val matches: (ItemStack) -> Boolean,
) {
    init {
        require(!pottedBlock.air()) { "pottedBlock must not be air" }
        require(pottedBlock.key() != Block.FLOWER_POT.key()) { "pottedBlock must not be the empty flower pot" }
    }
}

/**
 * Registry for vanilla and custom flower pot plants.
 *
 * Custom plants should be registered before the default block behaviour/placement rules are
 * registered so that the potted block also receives the support placement rule.
 */
object CustomPlants {
    private const val POTTED_PREFIX = "potted_"
    private val lock = Any()
    private val entries = CopyOnWriteArrayList<CustomPlant>()
    private val byBlock = ConcurrentHashMap<Key, CustomPlant>()

    @Volatile
    private var defaultsLoaded = false

    @JvmStatic
    fun register(
        item: Material,
        pottedBlock: Block,
    ) = register(item, pottedBlock, ItemStack.of(item))

    @JvmStatic
    fun register(
        item: Material,
        pottedBlock: Block,
        drop: Material,
    ) = register(item, pottedBlock, ItemStack.of(drop))

    @JvmStatic
    fun register(
        item: Material,
        pottedBlock: Block,
        drop: ItemStack,
    ) = register({ it.material() == item }, pottedBlock, drop)

    @JvmStatic
    fun register(
        matches: (ItemStack) -> Boolean,
        pottedBlock: Block,
        drop: ItemStack,
    ) {
        add(CustomPlant(pottedBlock, drop, matches))
    }

    @JvmStatic
    fun register(plant: CustomPlant) {
        add(plant)
    }

    fun findForItem(item: ItemStack): CustomPlant? {
        ensureDefaults()
        return entries.lastOrNull { it.matches(item) }
    }

    fun findForBlock(block: Block): CustomPlant? {
        ensureDefaults()
        return byBlock[block.key()]
    }

    fun allPottedBlocks(): Collection<Block> {
        ensureDefaults()
        return byBlock.values.map { it.pottedBlock }
    }

    fun resetDefaults() {
        synchronized(lock) {
            entries.clear()
            byBlock.clear()
            defaultsLoaded = false
        }
        ensureDefaults()
    }

    private fun add(plant: CustomPlant) {
        ensureDefaults()
        synchronized(lock) {
            entries.removeAll { it.pottedBlock.key() == plant.pottedBlock.key() }
            entries.add(plant)
            byBlock[plant.pottedBlock.key()] = plant
        }
        installHandler(plant.pottedBlock)
    }

    private fun ensureDefaults() {
        if (defaultsLoaded) return
        synchronized(lock) {
            if (defaultsLoaded) return
            defaultsLoaded = true
            loadDefaults()
        }
    }

    private fun loadDefaults() {
        for (block in Block.values()) {
            if (!block.key().asString().startsWith("minecraft:$POTTED_PREFIX")) continue

            val plantKey = Key.key("minecraft", block.key().value().removePrefix(POTTED_PREFIX))
            val material = Material.fromKey(plantKey) ?: continue
            val plant = CustomPlant(block, ItemStack.of(material)) { it.material() == material }
            entries.add(plant)
            byBlock[block.key()] = plant
        }
    }

    private fun installHandler(block: Block) {
        if (MinecraftServer.process() == null) return
        val key = block.key()
        MinecraftServer.getBlockManager().registerHandler(key.asString()) { FlowerPotHandler(key) }
    }
}

class FlowerPotHandler(
    private val key: Key = Key.key("minecraft:flower_pot"),
) : BlockHandler {
    override fun getKey(): Key = key

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val block = interaction.block
        val instance = interaction.instance
        val position = interaction.blockPosition

        if (block.key() == Block.FLOWER_POT.key()) {
            val item = interaction.player.getItemInHand(interaction.hand)
            val plant = CustomPlants.findForItem(item) ?: return true
            instance.setBlock(position, plant.pottedBlock.withHandler(handlerFor(plant.pottedBlock)))
            consumeItem(interaction.player, interaction.hand, item)
            return false
        }

        takePlant(instance, position, block)
        return false
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        val block = destroy.block
        val newBlock = destroy.newBlock
        if (newBlock.key() == block.key()) return
        if (block.key() == Block.FLOWER_POT.key() && CustomPlants.findForBlock(newBlock) != null) return
        if (newBlock.key() == Block.FLOWER_POT.key() && block.key() != Block.FLOWER_POT.key()) return

        CustomPlants
            .findForBlock(block)
            ?.drop
            ?.takeUnless { it.isAir }
            ?.let { DroppedItemFactory.maybeDrop(destroy.instance, destroy.blockPosition, it) }
        DroppedItemFactory.maybeDrop(destroy.instance, destroy.blockPosition, Block.FLOWER_POT)
    }

    private fun takePlant(
        instance: Instance,
        position: Point,
        block: Block,
    ) {
        CustomPlants
            .findForBlock(block)
            ?.drop
            ?.takeUnless { it.isAir }
            ?.let { DroppedItemFactory.maybeDrop(instance, position, it) }
        instance.setBlock(position, Block.FLOWER_POT.withHandler(handlerFor(Block.FLOWER_POT)))
    }

    private fun handlerFor(block: Block): FlowerPotHandler = FlowerPotHandler(block.key())

    private fun consumeItem(
        player: Player,
        hand: PlayerHand,
        item: ItemStack,
    ) {
        if (player.gameMode == GameMode.CREATIVE) return
        player.setItemInHand(hand, if (item.amount() <= 1) ItemStack.AIR else item.withAmount(item.amount() - 1))
    }
}
