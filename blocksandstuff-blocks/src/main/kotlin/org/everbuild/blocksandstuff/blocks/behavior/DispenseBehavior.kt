package org.everbuild.blocksandstuff.blocks.behavior

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.blocksandstuff.common.blockinventory.PhysicalInventory
import org.everbuild.blocksandstuff.redstone.RedstoneRegistrations

fun interface DispenseBehavior {
    /** @return true when the item was handled, false to fall back to dropper-style ejection. */
    fun dispense(context: DispenseContext): Boolean
}

class DispenseContext internal constructor(
    val instance: Instance,
    val position: Point,
    val inventory: PhysicalInventory,
    val slot: Int,
    val item: ItemStack,
    val facing: BlockFace,
) {
    fun setSlot(replacement: ItemStack) {
        inventory.transact { set -> set(slot, replacement) }
        notifyRedstone()
    }

    fun consumeOne() {
        inventory.transact { set ->
            val current = inventory.getItemStack(slot)
            set(slot, if (current.amount() > 1) current.withAmount(current.amount() - 1) else ItemStack.AIR)
        }
        notifyRedstone()
    }

    fun ejectOne() {
        val current = inventory.getItemStack(slot)
        if (current.isAir) return

        consumeOne()
        spawnItemEntity(instance, position, facing, current.withAmount(1))
    }

    fun placeFluid(fluid: Block): Boolean {
        val target = position.relative(facing)
        val current = instance.getBlock(target)
        if (!current.air() && !current.replaceable()) return false

        return instance.placeBlock(BlockHandler.Placement(fluid, current, instance, target))
    }

    fun pickUpFluid(fluid: Block): Boolean {
        val target = position.relative(facing)
        val current = instance.getBlock(target)
        if (!current.compare(fluid) || !isSource(current)) return false

        instance.setBlock(target, Block.AIR, true)
        return true
    }

    private fun isSource(block: Block): Boolean = (block.getProperty("level")?.toIntOrNull() ?: 0) == 0

    private fun notifyRedstone() {
        RedstoneRegistrations.notifyContainerChanged(instance, position)
    }
}

object DispenseBehaviors {
    private data class Entry(
        val matches: (ItemStack) -> Boolean,
        val behavior: DispenseBehavior,
    )

    private val entries = mutableListOf<Entry>()

    init {
        resetDefaults()
    }

    /** Registers or overrides the behavior for [material]. The latest registration wins. */
    fun register(
        material: Material,
        behavior: DispenseBehavior,
    ) {
        register({ it.material() == material }, behavior)
    }

    /** Registers or overrides a behavior for any matching item. The latest registration wins. */
    fun register(
        matches: (ItemStack) -> Boolean,
        behavior: DispenseBehavior,
    ) {
        entries.add(Entry(matches, behavior))
    }

    fun find(item: ItemStack): DispenseBehavior? = entries.lastOrNull { it.matches(item) }?.behavior

    fun resetDefaults() {
        entries.clear()
        register(Material.WATER_BUCKET) { context ->
            if (!context.placeFluid(Block.WATER)) return@register false
            context.setSlot(ItemStack.of(Material.BUCKET))
            true
        }
        register(Material.LAVA_BUCKET) { context ->
            if (!context.placeFluid(Block.LAVA)) return@register false
            context.setSlot(ItemStack.of(Material.BUCKET))
            true
        }
        register(Material.POWDER_SNOW_BUCKET) { context ->
            if (!context.placeFluid(Block.POWDER_SNOW)) return@register false
            context.setSlot(ItemStack.of(Material.BUCKET))
            true
        }
        register(Material.BUCKET) { context ->
            when {
                context.pickUpFluid(Block.WATER) -> {
                    context.setSlot(ItemStack.of(Material.WATER_BUCKET))
                    true
                }

                context.pickUpFluid(Block.LAVA) -> {
                    context.setSlot(ItemStack.of(Material.LAVA_BUCKET))
                    true
                }

                context.pickUpFluid(Block.POWDER_SNOW) -> {
                    context.setSlot(ItemStack.of(Material.POWDER_SNOW_BUCKET))
                    true
                }

                else -> {
                    false
                }
            }
        }
    }
}
